package com.swiftapp_backend.matchingservice.matching.application.service;

import com.swiftapp_backend.matchingservice.matching.application.config.MatchingProperties;
import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverOfferStatus;
import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStatus;
import com.swiftapp_backend.matchingservice.matching.domain.events.DriverMatchedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverCandidate;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverOffer;
import com.swiftapp_backend.matchingservice.matching.domain.models.MatchingState;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverLockRepository;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverMatchingRepository;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverOfferRepository;
import com.swiftapp_backend.matchingservice.matching.domain.repository.MatchingStateRepository;
import com.swiftapp_backend.matchingservice.matching.domain.repository.MatchingTimeoutRepository;
import com.swiftapp_backend.matchingservice.matching.application.port.MatchingEventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Lifecycle of one offer round. A ride targets one driver at a time; the
 * ride-level status is the arbiter for the accept/timeout race. Only one
 * transition out of PENDING is allowed by the atomic Redis script.
 *
 * <p>A driver who declines is released immediately and marked attempted.
 * The next untried driver is selected on rejection or expiry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverOfferService {

    private final DriverOfferRepository offerRepository;

    private final MatchingStateRepository stateRepository;

    private final DriverLockRepository lockRepository;

    private final MatchingTimeoutRepository timeoutRepository;

    private final DriverMatchingRepository driverMatchingRepository;

    private final MatchingEventPublisherPort eventPublisher;

    private final MatchingProperties properties;

    /**
     * One PENDING offer for this matching attempt.
     */
    public List<DriverOffer> createOffers(Long rideId,
                                         List<DriverCandidate> candidates,
                                         int attempt) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofSeconds(properties.getOfferTtlSeconds()));
        List<DriverOffer> offers = candidates.stream()
                .map(candidate -> DriverOffer.builder()
                        .rideId(rideId)
                        .driverId(candidate.getDriverId())
                        .status(DriverOfferStatus.PENDING)
                        .attempt(attempt)
                        .createdAt(now)
                        .expiresAt(expiresAt)
                        .build())
                .toList();
        double radiusKm = properties.radiusForAttempt(attempt);
        if (!offerRepository.saveBroadcast(rideId, offers, radiusKm)) {
            log.info("Skipped offer round for cancelled ride {}", rideId);
            return List.of();
        }
        timeoutRepository.scheduleTimeout(rideId, expiresAt);
        log.info("Broadcast created: ride={} drivers={} attempt={} radiusKm={} expiresAt={}",
                rideId, offers.size(), attempt, radiusKm, expiresAt);
        return offers;
    }

    /** Fences accept and new offer rounds before rider cancellation returns. */
    public DriverOfferRepository.CancelOutcome cancelPendingRide(Long rideId) {
        DriverOfferRepository.CancelOutcome result = offerRepository.cancelPendingRide(rideId);
        if (result == DriverOfferRepository.CancelOutcome.CANCELLED) {
            cleanupCancelledRide(rideId);
        }
        return result;
    }

    /** Applies the durable rider cancellation event after database commit. */
    public void recordRideCancelled(Long rideId) {
        offerRepository.recordCancellation(rideId);
        cleanupCancelledRide(rideId);
    }

    private void cleanupCancelledRide(Long rideId) {
        timeoutRepository.cancelTimeout(rideId);
        Set<Long> offered = offerRepository.findOfferedDriverIds(rideId);
        offered.forEach(id -> lockRepository.releaseLock(id, rideId));
        if (!offered.isEmpty()) {
            eventPublisher.publishOfferWithdrawn(rideId, List.copyOf(offered), "RIDER_CANCELLED");
        }
        stateRepository.findByRideId(rideId).ifPresent(state -> {
            state.setStatus(MatchingStatus.CANCELLED);
            state.setUpdatedAt(Instant.now());
            stateRepository.save(state);
        });
    }

    public OfferResolution acceptOffer(Long rideId, Long driverId) {
        return acceptOffer(rideId, driverId, null);
    }

    public OfferResolution acceptOffer(Long rideId, Long driverId, Long vehicleId) {
        Optional<DriverOffer> existing = offerRepository.findOffer(rideId, driverId);
        if (existing.isEmpty()) {
            return offerRepository.findOfferedDriverIds(rideId).isEmpty()
                    ? OfferResolution.NO_OFFER
                    : OfferResolution.WRONG_DRIVER;
        }
        MatchingState preAcceptState = stateRepository.findByRideId(rideId).orElse(null);
        DriverMatchedEvent matched = DriverMatchedEvent.builder()
                .rideId(rideId)
                .driverId(driverId)
                .vehicleId(vehicleId)
                .authUserId(preAcceptState != null ? preAcceptState.getRiderId() : null)
                .isActive(true)
                .build();
        if (!offerRepository.tryAccept(rideId, driverId, matched)) {
            log.info("Accept for ride {} by driver {} arrived after the ride already settled",
                    rideId, driverId);
            return OfferResolution.ALREADY_SETTLED;
        }

        timeoutRepository.cancelTimeout(rideId);
        Set<Long> offered = offerRepository.findOfferedDriverIds(rideId);
        // The winner is no longer matchable: flip them out of ACTIVE so the
        // GEO filter skips them. Release every targeted driver's lock
        // explicitly (atomic compare-and-delete) so the rest are immediately
        // offerable again - no waiting on the TTL.
        offered.forEach(id -> lockRepository.releaseLock(id, rideId));
        driverMatchingRepository.updateDriverStatus(driverId, DriverStatusEventEnum.ALREADY_ACQUIRED);

        stateRepository.findByRideId(rideId).ifPresent(state -> {
            state.setStatus(MatchingStatus.MATCHED);
            state.setDriverId(driverId);
            state.setUpdatedAt(Instant.now());
            stateRepository.save(state);
        });

        // Tell the other targeted drivers their offer is gone.
        Set<Long> losers = new HashSet<>(offered);
        losers.remove(driverId);
        if (!losers.isEmpty()) {
            eventPublisher.publishOfferWithdrawn(rideId, List.copyOf(losers),
                    "ACCEPTED_BY_OTHER_DRIVER");
        }

        eventPublisher.publishDriverMatched(matched);

        log.info("Ride {} matched to driver {} (first of {} targeted drivers)",
                rideId, driverId, offered.size());
        return OfferResolution.ACCEPTED;
    }

    public OfferResolution rejectOffer(Long rideId, Long driverId, String reason) {
        Optional<DriverOffer> existing = offerRepository.findOffer(rideId, driverId);
        if (existing.isEmpty()) {
            return offerRepository.findOfferedDriverIds(rideId).isEmpty()
                    ? OfferResolution.NO_OFFER
                    : OfferResolution.WRONG_DRIVER;
        }
        DriverOfferRepository.DeclineOutcome outcome =
                offerRepository.recordDecline(rideId, driverId);
        if (outcome == DriverOfferRepository.DeclineOutcome.NOT_APPLICABLE) {
            return OfferResolution.ALREADY_SETTLED;
        }

        // This driver is free again immediately and must not be re-targeted
        // by the next attempt of this ride.
        lockRepository.releaseLock(driverId, rideId);
        stateRepository.addAttemptedDriver(rideId, driverId);

        if (outcome == DriverOfferRepository.DeclineOutcome.ALL_DECLINED) {
            // Exactly one thread may settle the all-declined round.
            if (!offerRepository.compareAndSetStatus(rideId,
                    DriverOfferStatus.PENDING, DriverOfferStatus.REJECTED)) {
                return OfferResolution.ALREADY_SETTLED;
            }
            timeoutRepository.cancelTimeout(rideId);
            Set<Long> offered = offerRepository.findOfferedDriverIds(rideId);
            offered.forEach(id -> lockRepository.releaseLock(id, rideId));
            stateRepository.findByRideId(rideId).ifPresent(state -> {
                state.setStatus(MatchingStatus.REASSIGNING);
                state.setUpdatedAt(Instant.now());
                stateRepository.save(state);
            });
            log.info("All {} targeted drivers declined ride {}", offered.size(), rideId);
            return OfferResolution.REJECTED;
        }

        log.info("Driver {} declined ride {} (reason={}); waiting on the rest",
                driverId, rideId, reason);
        return OfferResolution.DECLINED;
    }

    /**
     * Called by the timeout worker when the offer window lapses.
     * If a driver accepted concurrently, the CAS fails and this
     * becomes a no-op: the accept already won.
     */
    public OfferResolution expireOffer(Long rideId) {
        boolean transitioned = offerRepository.expireIfDue(rideId, Instant.now());
        if (!transitioned) {
            return OfferResolution.ALREADY_SETTLED;
        }

        timeoutRepository.cancelTimeout(rideId);
        Set<Long> offered = offerRepository.findOfferedDriverIds(rideId);
        offered.forEach(id -> {
            lockRepository.releaseLock(id, rideId);
            stateRepository.addAttemptedDriver(rideId, id);
        });
        if (!offered.isEmpty()) {
            eventPublisher.publishOfferWithdrawn(rideId, List.copyOf(offered), "EXPIRED");
        }
        stateRepository.findByRideId(rideId).ifPresent(state -> {
            state.setStatus(MatchingStatus.TIMEOUT);
            state.setUpdatedAt(Instant.now());
            stateRepository.save(state);
        });
        log.info("Broadcast for ride {} expired with no accept ({} drivers targeted)",
                rideId, offered.size());
        return OfferResolution.EXPIRED;
    }
}
