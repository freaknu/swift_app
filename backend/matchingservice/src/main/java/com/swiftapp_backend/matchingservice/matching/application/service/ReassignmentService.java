package com.swiftapp_backend.matchingservice.matching.application.service;

import com.swiftapp_backend.matchingservice.matching.application.config.MatchingProperties;
import com.swiftapp_backend.matchingservice.matching.domain.dto.MatchingContext;
import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStatus;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideOfferBroadcastEvent;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideMatchFailedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverCandidate;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverOffer;
import com.swiftapp_backend.matchingservice.matching.domain.models.MatchingState;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverCandidateRepository;
import com.swiftapp_backend.matchingservice.matching.domain.repository.MatchingStateRepository;
import com.swiftapp_backend.matchingservice.matching.application.port.MatchingEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReassignmentService {

    private final MatchingStateRepository stateRepository;

    private final DriverCandidateRepository candidateRepository;

    private final DriverSelectionService selectionService;

    private final DriverLockService lockService;

    private final DriverOfferService offerService;

    private final MatchingEventPublisherPort eventPublisher;

    private final MatchingProperties properties;

    /** First attempt for a fresh ride request. */
    public void startAttempt(Long rideId) {
        attemptMatch(rideId, 1);
    }

    /** Next attempt after all drivers declined, or the offer window expired. */
    public void reassign(Long rideId) {
        MatchingState state = stateRepository.findByRideId(rideId).orElse(null);
        if (state == null) {
            log.warn("Reassign called for unknown ride {}", rideId);
            return;
        }
        if (state.isTerminal()) {
            log.debug("Reassign called for terminal ride {} ({})", rideId, state.getStatus());
            return;
        }
        attemptMatch(rideId, state.getAttempt() + 1);
    }

    private void attemptMatch(Long rideId, int attempt) {
        MatchingState state = stateRepository.findByRideId(rideId).orElse(null);
        if (state == null || state.isTerminal()) {
            return;
        }
        if (attempt > properties.getMaxAttempts()) {
            failRide(state, "NO_AVAILABLE_DRIVER");
            return;
        }

        double radiusKm = properties.radiusForAttempt(attempt);
        Set<Long> attempted = stateRepository.attemptedDrivers(rideId);

        List<DriverCandidate> candidates = candidateRepository.findCandidates(
                        state.getPickupLatitude(),
                        state.getPickupLongitude(),
                        state.getVehicleType(),
                        radiusKm,
                        properties.getCandidateLimit()).stream()
                .filter(c -> !attempted.contains(c.getDriverId()))
                .toList();

        if (candidates.isEmpty()) {
            log.info("No fresh candidates for ride {} at attempt {} ({} km tried)",
                    rideId, attempt, radiusKm);
            if (attempt >= properties.getMaxAttempts()) {
                failRide(state, "NO_AVAILABLE_DRIVER");
            } else {
                // Widen the radius on the next attempt.
                state.setAttempt(attempt);
                state.setRadiusKm(radiusKm);
                state.setStatus(MatchingStatus.REASSIGNING);
                state.setUpdatedAt(Instant.now());
                stateRepository.save(state);
                attemptMatch(rideId, attempt + 1);
            }
            return;
        }

        MatchingContext context = MatchingContext.builder()
                .rideId(rideId)
                .riderId(state.getRiderId())
                .latitude(state.getPickupLatitude())
                .longitude(state.getPickupLongitude())
                .vehicleType(state.getVehicleType())
                .radiusKm(radiusKm)
                .attempt(attempt)
                .excludedDriverIds(attempted)
                .build();

        // Offer to exactly one eligible driver at a time. A declined or
        // expired offer advances to the next untried driver on the next
        // attempt; the lock prevents a second ride targeting this driver.
        List<DriverCandidate> targets = new ArrayList<>();
        for (DriverCandidate candidate : selectionService.orderCandidates(candidates, context)) {
            if (lockService.acquire(candidate.getDriverId(), rideId)) {
                targets.add(candidate);
                break;
            }
        }

        if (targets.isEmpty()) {
            // Every candidate was locked by another ride; try a wider radius.
            log.info("All {} candidates locked for ride {} at attempt {}, widening search",
                    candidates.size(), rideId, attempt);
            state.setAttempt(attempt);
            state.setRadiusKm(radiusKm);
            state.setStatus(MatchingStatus.REASSIGNING);
            state.setUpdatedAt(Instant.now());
            stateRepository.save(state);
            attemptMatch(rideId, attempt + 1);
            return;
        }

        List<DriverOffer> offers = offerService.createOffers(rideId, targets, attempt);
        if (offers.isEmpty()) {
            targets.forEach(candidate -> lockService.release(candidate.getDriverId(), rideId));
            stateRepository.findByRideId(rideId).ifPresent(current -> {
                current.setStatus(MatchingStatus.CANCELLED);
                current.setUpdatedAt(Instant.now());
                stateRepository.save(current);
            });
            return;
        }
        state.setStatus(MatchingStatus.OFFER_SENT);
        state.setDriverId(null);
        state.setAttempt(attempt);
        state.setRadiusKm(radiusKm);
        state.setUpdatedAt(Instant.now());
        stateRepository.save(state);
        eventPublisher.publishOfferBroadcast(buildBroadcastEvent(state, targets,
                offers.get(0).getExpiresAt(), radiusKm, attempt));
        log.info("Ride {} offered to {} driver (attempt {}, {} km)",
                rideId, targets.size(), attempt, radiusKm);
    }

    private RideOfferBroadcastEvent buildBroadcastEvent(MatchingState state,
                                                       List<DriverCandidate> targets,
                                                       java.time.Instant expiresAt,
                                                       double radiusKm,
                                                       int attempt) {
        List<RideOfferBroadcastEvent.OfferTarget> offerTargets = targets.stream()
                .map(candidate -> RideOfferBroadcastEvent.OfferTarget.builder()
                        .driverId(candidate.getDriverId())
                        .distanceKm(candidate.getDistanceKm())
                        .build())
                .toList();
        return RideOfferBroadcastEvent.builder()
                .rideId(state.getRideId())
                .riderId(state.getRiderId())
                .pickupLatitude(state.getPickupLatitude())
                .pickupLongitude(state.getPickupLongitude())
                .destinationLatitude(state.getDestinationLatitude())
                .destinationLongitude(state.getDestinationLongitude())
                .estimatedDistance(state.getEstimatedDistance())
                .estimatedDuration(state.getEstimatedDuration())
                .totalFare(state.getTotalFare())
                .currency(state.getCurrency())
                .vehicleType(state.getVehicleType())
                .attempt(attempt)
                .radiusKm(radiusKm)
                .expiresAt(expiresAt != null ? expiresAt.toString() : null)
                .targets(offerTargets)
                .build();
    }

    private void failRide(MatchingState state, String reason) {
        state.setStatus(MatchingStatus.FAILED);
        state.setUpdatedAt(Instant.now());
        if (!stateRepository.saveFailed(state,
                new RideMatchFailedEvent(state.getRideId(), state.getRiderId(), reason))) {
            log.info("Skipped matching failure for cancelled ride {}", state.getRideId());
            return;
        }
        eventPublisher.publishMatchFailed(state.getRideId(), state.getRiderId(), reason);
        log.info("Ride {} matching failed: {}", state.getRideId(), reason);
    }
}
