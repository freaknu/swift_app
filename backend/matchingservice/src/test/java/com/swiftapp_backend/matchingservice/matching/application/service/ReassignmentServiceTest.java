package com.swiftapp_backend.matchingservice.matching.application.service;

import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStatus;
import com.swiftapp_backend.matchingservice.matching.domain.models.MatchingState;
import com.swiftapp_backend.matchingservice.matching.support.MatchingTestHarness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers broadcast targeting, lock contention, attempted-driver
 * exclusion, radius widening and ride failure.
 */
class ReassignmentServiceTest {

    private MatchingTestHarness h;

    @BeforeEach
    void setUp() {
        h = new MatchingTestHarness();
    }

    @Test
    void offersToOneDriverThenAdvancesAfterDecline() {
        h.newState(5001L);
        h.candidateRepo.candidates.add(MatchingTestHarness.candidate(101L, 0.4));
        h.candidateRepo.candidates.add(MatchingTestHarness.candidate(102L, 0.8));

        h.reassignmentService.startAttempt(5001L);

        MatchingState state = h.stateRepo.findByRideId(5001L).orElseThrow();
        assertEquals(MatchingStatus.OFFER_SENT, state.getStatus());
        assertEquals(1, state.getAttempt());
        assertEquals(5.0, state.getRadiusKm());
        assertEquals(1, h.publisher.broadcasts.size());
        var broadcast = h.publisher.broadcasts.get(0);
        assertEquals(1, broadcast.attempt());
        assertEquals(5.0, broadcast.radiusKm());
        assertEquals(List.of(101L), broadcast.driverIds());
        assertTrue(h.lockRepo.isLocked(101L));
        assertTrue(!h.lockRepo.isLocked(102L));

        assertEquals(OfferResolution.REJECTED,
                h.offerService.rejectOffer(5001L, 101L, "declined"));
        h.reassignmentService.reassign(5001L);
        assertEquals(2, h.publisher.broadcasts.size());
        assertEquals(List.of(102L), h.publisher.broadcasts.get(1).driverIds());
        assertEquals(OfferResolution.WRONG_DRIVER,
                h.offerService.acceptOffer(5001L, 101L));
    }

    @Test
    void skipsAttemptedAndLockedDrivers() {
        h.newState(5001L);
        h.stateRepo.addAttemptedDriver(5001L, 101L);
        h.lockRepo.forceLock(102L, 9999L); // held by another ride
        h.candidateRepo.candidates.add(MatchingTestHarness.candidate(101L, 0.3));
        h.candidateRepo.candidates.add(MatchingTestHarness.candidate(102L, 0.5));
        h.candidateRepo.candidates.add(MatchingTestHarness.candidate(103L, 0.7));

        h.reassignmentService.startAttempt(5001L);

        assertEquals(1, h.publisher.broadcasts.size());
        assertEquals(List.of(103L), h.publisher.broadcasts.get(0).driverIds());
    }

    @Test
    void radiusGrowsOnExpiryUntilAccepted() {
        h.properties.setMaxAttempts(3);
        h.properties.setRadiiKm(List.of(5.0, 7.0, 15.0));
        h.newState(5001L);
        h.candidateRepo.candidates.add(MatchingTestHarness.candidate(101L, 0.4));

        h.properties.setOfferTtlSeconds(0);
        h.reassignmentService.startAttempt(5001L);
        // Nobody accepts: the window expires, the radius widens.
        h.offerService.expireOffer(5001L);
        h.properties.setOfferTtlSeconds(15);
        h.candidateRepo.candidates.add(MatchingTestHarness.candidate(102L, 0.9));
        h.reassignmentService.reassign(5001L);

        assertEquals(2, h.publisher.broadcasts.size());
        assertEquals(5.0, h.publisher.broadcasts.get(0).radiusKm());
        assertEquals(7.0, h.publisher.broadcasts.get(1).radiusKm());
        assertEquals(2, h.publisher.broadcasts.get(1).attempt());
        // Driver 101 was attempted in round one, so round two only targets 102.
        assertEquals(List.of(102L), h.publisher.broadcasts.get(1).driverIds());
        assertEquals(1, h.publisher.withdrawn.size());
        assertEquals("EXPIRED", h.publisher.withdrawn.get(0).reason());
    }

    @Test
    void widensRadiusAcrossAttemptsThenFails() {
        h.properties.setMaxAttempts(2);
        h.properties.setRadiiKm(java.util.List.of(5.0, 10.0));
        h.newState(5001L);
        // no candidates at all

        h.reassignmentService.startAttempt(5001L);

        assertEquals(java.util.List.of(5.0, 10.0), h.candidateRepo.requestedRadii);
        MatchingState state = h.stateRepo.findByRideId(5001L).orElseThrow();
        assertEquals(MatchingStatus.FAILED, state.getStatus());
        assertEquals(1, h.publisher.matchFailed.size());
        assertEquals("NO_AVAILABLE_DRIVER", h.publisher.matchFailed.get(0).reason());
    }

    @Test
    void reassignSkipsTerminalRides() {
        MatchingState state = h.newState(5001L);
        state.setStatus(MatchingStatus.MATCHED);
        h.stateRepo.save(state);
        h.candidateRepo.candidates.add(MatchingTestHarness.candidate(101L, 0.4));

        h.reassignmentService.reassign(5001L);

        assertTrue(h.publisher.broadcasts.isEmpty());
    }
}
