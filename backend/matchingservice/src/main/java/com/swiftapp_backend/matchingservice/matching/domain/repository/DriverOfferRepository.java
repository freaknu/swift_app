package com.swiftapp_backend.matchingservice.matching.domain.repository;

import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverOfferStatus;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverOffer;
import com.swiftapp_backend.matchingservice.matching.domain.events.DriverMatchedEvent;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.Instant;

/**
 * Persistence for driver offers. A matching attempt targets one driver.
 * The ride-level status arbitrates between accept, reject and timeout.
 */
public interface DriverOfferRepository {

    /** Outcome of recording one driver's decline. */
    enum DeclineOutcome {
        /** Decline recorded; other drivers are still pending. */
        RECORDED,
        /** Every targeted driver has now declined. */
        ALL_DECLINED,
        /** Ride is not PENDING or the driver was not targeted. */
        NOT_APPLICABLE
    }

    /**
     * Stores the current PENDING offer and ride-level state.
     */
    /** Returns false when the ride was already cancelled. */
    boolean saveBroadcast(Long rideId, List<DriverOffer> offers, double radiusKm);

    enum CancelOutcome { CANCELLED, ALREADY_ACCEPTED }

    /** Atomically fences all future offers and accepts unless an accept won first. */
    CancelOutcome cancelPendingRide(Long rideId);

    /** Records an already-persisted cancellation, including assigned rides. */
    void recordCancellation(Long rideId);

    boolean isRideCancelled(Long rideId);

    /** DriverIds targeted by the current broadcast for a ride. */
    Set<Long> findOfferedDriverIds(Long rideId);

    /** One driver's offer within a ride broadcast. */
    Optional<DriverOffer> findOffer(Long rideId, Long driverId);

    /**
     * Atomically accepts on behalf of one driver: succeeds only when the
     * ride is still PENDING and the driver was targeted. Records the
     * winner. First accept wins.
     */
    boolean tryAccept(Long rideId, Long driverId, DriverMatchedEvent matchedEvent);

    /** Matched events committed with the accept CAS and awaiting Kafka ack. */
    List<DriverMatchedEvent> pendingMatchedEvents(int limit);

    void acknowledgeMatchedEvent(Long rideId);

    /**
     * Records one driver's decline. The driver is added to the declined
     * set; when the declined set covers every targeted driver the ride
     * is ready for reassignment.
     */
    DeclineOutcome recordDecline(Long rideId, Long driverId);

    /**
     * Atomically moves the ride-level offer status from {@code expected}
     * to {@code next}.
     *
     * @return true if the transition happened, false if the ride was
     *         already moved by someone else (accept vs timeout race).
     */
    boolean compareAndSetStatus(Long rideId,
                                DriverOfferStatus expected,
                                DriverOfferStatus next);

    /** Expire only the current round, and only after its own deadline. */
    boolean expireIfDue(Long rideId, Instant now);

    void delete(Long rideId);
}
