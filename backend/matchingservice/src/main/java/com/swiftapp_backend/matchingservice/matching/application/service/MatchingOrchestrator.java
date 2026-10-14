package com.swiftapp_backend.matchingservice.matching.application.service;

import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStatus;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideRequestedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.models.MatchingState;
import com.swiftapp_backend.matchingservice.matching.domain.repository.MatchingStateRepository;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverOfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Entry point for a ride.requested event. Idempotent: a redelivered
 * event for a ride that is already being matched (or finished) is ignored.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingOrchestrator {

    private final MatchingStateRepository stateRepository;

    private final DriverOfferRepository offerRepository;

    private final ReassignmentService reassignmentService;

    public void startMatching(RideRequestedEvent event) {
        if (event.getRideId() == null) {
            log.warn("Ignoring ride request without rideId");
            return;
        }
        if (event.getPickupLatitude() == null || event.getPickupLongitude() == null) {
            log.warn("Ignoring ride {} without pickup coordinates", event.getRideId());
            return;
        }
        if (offerRepository.isRideCancelled(event.getRideId())) {
            log.info("Ignoring request for cancelled ride {}", event.getRideId());
            return;
        }

        Optional<MatchingState> existing = stateRepository.findByRideId(event.getRideId());
        if (existing.isPresent()) {
            MatchingState state = existing.get();
            log.info("Ride {} already has matching state ({}), ignoring duplicate request",
                    event.getRideId(), state.getStatus());
            return;
        }

        Instant now = Instant.now();
        MatchingState state = MatchingState.builder()
                .rideId(event.getRideId())
                .riderId(event.getRiderId())
                .pickupLatitude(event.getPickupLatitude())
                .pickupLongitude(event.getPickupLongitude())
                .destinationLatitude(event.getDestinationLatitude())
                .destinationLongitude(event.getDestinationLongitude())
                .estimatedDistance(event.getEstimatedDistance())
                .estimatedDuration(event.getEstimatedDuration())
                .totalFare(event.getTotalFare())
                .currency(event.getCurrency())
                .vehicleType(event.getVehicleType())
                .status(MatchingStatus.SEARCHING)
                .attempt(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        stateRepository.save(state);

        if (offerRepository.isRideCancelled(event.getRideId())) {
            state.setStatus(MatchingStatus.CANCELLED);
            state.setUpdatedAt(Instant.now());
            stateRepository.save(state);
            return;
        }

        log.info("Starting matching for ride {} (rider {}, vehicle {})",
                event.getRideId(), event.getRiderId(), event.getVehicleType());
        reassignmentService.startAttempt(event.getRideId());
    }
}
