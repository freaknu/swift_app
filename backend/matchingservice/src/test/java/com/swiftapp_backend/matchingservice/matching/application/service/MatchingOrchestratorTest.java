package com.swiftapp_backend.matchingservice.matching.application.service;

import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStatus;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideRequestedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.models.MatchingState;
import com.swiftapp_backend.matchingservice.matching.support.MatchingTestHarness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers orchestrator idempotency: duplicate ride.requested deliveries
 * must not create a second broadcast.
 */
class MatchingOrchestratorTest {

    private MatchingTestHarness h;

    @BeforeEach
    void setUp() {
        h = new MatchingTestHarness();
    }

    @Test
    void happyPathStartsMatchingAndBroadcasts() {
        h.candidateRepo.candidates.add(MatchingTestHarness.candidate(101L, 0.4));

        h.orchestrator.startMatching(request(5001L));

        MatchingState state = h.stateRepo.findByRideId(5001L).orElseThrow();
        assertEquals(MatchingStatus.OFFER_SENT, state.getStatus());
        assertEquals(1, h.publisher.broadcasts.size());
        assertEquals(List.of(101L), h.publisher.broadcasts.get(0).driverIds());
        assertEquals(Set.of(101L), h.offerRepo.findOfferedDriverIds(5001L));
    }

    @Test
    void duplicateRequestWhileMatchingIsIgnored() {
        h.candidateRepo.candidates.add(MatchingTestHarness.candidate(101L, 0.4));

        h.orchestrator.startMatching(request(5001L));
        h.orchestrator.startMatching(request(5001L)); // redelivery

        assertEquals(1, h.publisher.broadcasts.size());
        assertEquals(1, h.stateRepo.findByRideId(5001L).orElseThrow().getAttempt());
    }

    @Test
    void requestWithoutRideIdIsIgnored() {
        h.orchestrator.startMatching(RideRequestedEvent.builder()
                .riderId(7L)
                .pickupLatitude(12.97)
                .pickupLongitude(77.59)
                .build());

        assertTrue(h.publisher.broadcasts.isEmpty());
    }

    @Test
    void requestWithoutCoordinatesIsIgnored() {
        h.orchestrator.startMatching(RideRequestedEvent.builder()
                .rideId(5001L)
                .riderId(7L)
                .build());

        assertTrue(h.publisher.broadcasts.isEmpty());
        assertTrue(h.stateRepo.findByRideId(5001L).isEmpty());
    }

    private static RideRequestedEvent request(Long rideId) {
        return RideRequestedEvent.builder()
                .rideId(rideId)
                .riderId(7L)
                .pickupLatitude(12.9716)
                .pickupLongitude(77.5946)
                .build();
    }
}
