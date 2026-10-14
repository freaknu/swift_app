package com.swiftapp_backend.matchingservice.matching.application.service;

import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverOfferStatus;
import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStatus;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverCandidate;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverOffer;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverOfferRepository;
import com.swiftapp_backend.matchingservice.matching.support.MatchingTestHarness;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the broadcast offer lifecycle: one PENDING offer per targeted
 * driver, first accept wins, declines accumulate until every targeted
 * driver has declined, and the accept-vs-timeout race.
 */
class DriverOfferServiceTest {

    private MatchingTestHarness h;

    @BeforeEach
    void setUp() {
        h = new MatchingTestHarness();
    }

    private static List<DriverCandidate> drivers(Long... ids) {
        return Arrays.stream(ids)
                .map(id -> MatchingTestHarness.candidate(id, 0.4))
                .toList();
    }

    @Test
    void createOffersArePendingAndScheduled() {
        h.newState(5001L);

        List<DriverOffer> offers = h.offerService.createOffers(5001L, drivers(101L, 102L), 1);

        assertEquals(2, offers.size());
        assertTrue(offers.stream()
                .allMatch(o -> o.getStatus() == DriverOfferStatus.PENDING));
        assertEquals(Set.of(101L, 102L), h.offerRepo.findOfferedDriverIds(5001L));
        assertTrue(h.timeoutRepo.hasTimeout(5001L));
    }

    @Test
    void firstAcceptWinsAndWithdrawsTheRest() {
        h.newState(5001L);
        h.offerService.createOffers(5001L, drivers(101L, 102L), 1);

        assertEquals(OfferResolution.ACCEPTED, h.offerService.acceptOffer(5001L, 101L));

        assertEquals(MatchingStatus.MATCHED,
                h.stateRepo.findByRideId(5001L).orElseThrow().getStatus());
        assertEquals(101L,
                h.stateRepo.findByRideId(5001L).orElseThrow().getDriverId());
        assertEquals(1, h.publisher.matched.size());
        assertEquals(1, h.offerRepo.pendingMatchedEvents(10).size());
        assertEquals(101L, h.publisher.matched.get(0).getDriverId());
        assertEquals(DriverStatusEventEnum.ALREADY_ACQUIRED,
                h.driverRepo.statusUpdates.get(101L));
        assertFalse(h.timeoutRepo.hasTimeout(5001L));
        // The other targeted driver is told their offer is gone.
        assertEquals(1, h.publisher.withdrawn.size());
        assertEquals(Set.of(102L),
                Set.copyOf(h.publisher.withdrawn.get(0).driverIds()));
        assertEquals("ACCEPTED_BY_OTHER_DRIVER",
                h.publisher.withdrawn.get(0).reason());
        // A late accept by the loser settles nowhere.
        assertEquals(OfferResolution.ALREADY_SETTLED,
                h.offerService.acceptOffer(5001L, 102L));
        assertEquals(1, h.publisher.matched.size());
    }

    @Test
    void timeoutWinningFirstMakesLateAcceptSettled() {
        h.newState(5001L);
        h.properties.setOfferTtlSeconds(0);
        h.offerService.createOffers(5001L, drivers(101L, 102L), 1);

        assertEquals(OfferResolution.EXPIRED, h.offerService.expireOffer(5001L));
        assertEquals(OfferResolution.ALREADY_SETTLED,
                h.offerService.acceptOffer(5001L, 101L));

        // No driver.matched published, driver not flipped to ALREADY_ACQUIRED.
        assertTrue(h.publisher.matched.isEmpty());
        assertFalse(h.driverRepo.statusUpdates.containsKey(101L));
        assertEquals(1, h.publisher.withdrawn.size());
        assertEquals("EXPIRED", h.publisher.withdrawn.get(0).reason());
    }

    @Test
    void acceptWinningFirstMakesLateTimeoutNoOp() {
        h.newState(5001L);
        h.offerService.createOffers(5001L, drivers(101L), 1);

        assertEquals(OfferResolution.ACCEPTED, h.offerService.acceptOffer(5001L, 101L));
        assertEquals(OfferResolution.ALREADY_SETTLED, h.offerService.expireOffer(5001L));

        assertEquals(MatchingStatus.MATCHED,
                h.stateRepo.findByRideId(5001L).orElseThrow().getStatus());
    }

    @Test
    void wrongDriverCannotAccept() {
        h.newState(5001L);
        h.offerService.createOffers(5001L, drivers(101L), 1);

        assertEquals(OfferResolution.WRONG_DRIVER,
                h.offerService.acceptOffer(5001L, 102L));
    }

    @Test
    void singleDeclineKeepsWaitingForTheRest() {
        h.newState(5001L);
        h.lockRepo.forceLock(101L, 5001L);
        h.lockRepo.forceLock(102L, 5001L);
        h.offerService.createOffers(5001L, drivers(101L, 102L), 1);

        assertEquals(OfferResolution.DECLINED,
                h.offerService.rejectOffer(5001L, 101L, "busy"));

        // The declining driver is released and marked attempted; the ride
        // keeps waiting on the other driver - no reassignment yet.
        assertFalse(h.lockRepo.isLocked(101L));
        assertTrue(h.lockRepo.isLocked(102L));
        assertTrue(h.stateRepo.attemptedDrivers(5001L).contains(101L));
        assertTrue(h.timeoutRepo.hasTimeout(5001L));
        assertTrue(h.publisher.withdrawn.isEmpty());
    }

    @Test
    void allDeclinedTriggersReassign() {
        h.newState(5001L);
        h.lockRepo.forceLock(101L, 5001L);
        h.lockRepo.forceLock(102L, 5001L);
        h.offerService.createOffers(5001L, drivers(101L, 102L), 1);

        assertEquals(OfferResolution.DECLINED,
                h.offerService.rejectOffer(5001L, 101L, "busy"));
        assertEquals(OfferResolution.REJECTED,
                h.offerService.rejectOffer(5001L, 102L, "busy"));

        assertFalse(h.lockRepo.isLocked(101L));
        assertFalse(h.lockRepo.isLocked(102L));
        assertTrue(h.stateRepo.attemptedDrivers(5001L).containsAll(Set.of(101L, 102L)));
        assertEquals(MatchingStatus.REASSIGNING,
                h.stateRepo.findByRideId(5001L).orElseThrow().getStatus());
        assertFalse(h.timeoutRepo.hasTimeout(5001L));
        // A duplicate decline after the round settled is a no-op.
        assertEquals(OfferResolution.ALREADY_SETTLED,
                h.offerService.rejectOffer(5001L, 101L, "busy"));
    }

    @Test
    void rejectNeverStealsAnotherRidesLock() {
        h.newState(5001L);
        h.offerService.createOffers(5001L, drivers(101L), 1);
        // Lock expired and ride 5002 re-acquired driver 101 meanwhile.
        h.lockRepo.forceLock(101L, 5002L);

        assertEquals(OfferResolution.REJECTED,
                h.offerService.rejectOffer(5001L, 101L, "busy"));

        assertEquals(5002L, h.lockRepo.lockedBy(101L).orElseThrow());
    }

    @Test
    void acceptWithoutOfferIsNoOffer() {
        h.newState(5001L);
        assertEquals(OfferResolution.NO_OFFER, h.offerService.acceptOffer(5001L, 101L));
    }

    @Test
    void riderCancellationFencesImmediateAcceptAndWithdrawsOffer() {
        h.newState(5001L);
        h.lockRepo.forceLock(101L, 5001L);
        h.offerService.createOffers(5001L, drivers(101L), 1);

        assertEquals(DriverOfferRepository.CancelOutcome.CANCELLED,
                h.offerService.cancelPendingRide(5001L));
        assertEquals(OfferResolution.ALREADY_SETTLED,
                h.offerService.acceptOffer(5001L, 101L, 301L));
        assertEquals(MatchingStatus.CANCELLED,
                h.stateRepo.findByRideId(5001L).orElseThrow().getStatus());
        assertFalse(h.lockRepo.isLocked(101L));
        assertFalse(h.timeoutRepo.hasTimeout(5001L));
        assertTrue(h.publisher.matched.isEmpty());
        assertEquals("RIDER_CANCELLED", h.publisher.withdrawn.get(0).reason());
        assertTrue(h.offerService.createOffers(5001L, drivers(102L), 2).isEmpty());
    }

    @Test
    void acceptedOfferWinsConcurrentCancellation() {
        h.newState(5001L);
        h.offerService.createOffers(5001L, drivers(101L), 1);
        assertEquals(OfferResolution.ACCEPTED, h.offerService.acceptOffer(5001L, 101L, 301L));

        assertEquals(DriverOfferRepository.CancelOutcome.ALREADY_ACCEPTED,
                h.offerService.cancelPendingRide(5001L));
        assertEquals(MatchingStatus.MATCHED,
                h.stateRepo.findByRideId(5001L).orElseThrow().getStatus());
        assertFalse(h.offerRepo.isRideCancelled(5001L));
    }

    @Test
    void cancellationBeforeRequestPreventsLaterMatchingReplay() {
        h.offerService.cancelPendingRide(5001L);
        h.orchestrator.startMatching(com.swiftapp_backend.matchingservice.matching.domain.events.RideRequestedEvent
                .builder().rideId(5001L).riderId(7L)
                .pickupLatitude(12.9716).pickupLongitude(77.5946).build());

        assertTrue(h.stateRepo.findByRideId(5001L).isEmpty());
        assertTrue(h.publisher.broadcasts.isEmpty());
    }
}
