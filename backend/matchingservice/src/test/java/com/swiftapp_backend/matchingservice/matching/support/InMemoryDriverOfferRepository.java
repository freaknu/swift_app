package com.swiftapp_backend.matchingservice.matching.support;

import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverOfferStatus;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverOffer;
import com.swiftapp_backend.matchingservice.matching.domain.events.DriverMatchedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverOfferRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** In-memory fake with real compare-and-set semantics. */
public class InMemoryDriverOfferRepository implements DriverOfferRepository {

    private static class Broadcast {
        DriverOfferStatus status = DriverOfferStatus.PENDING;
        String winner = "";
        final Map<Long, DriverOffer> offers = new HashMap<>();
        final Set<Long> declined = new HashSet<>();
        double radiusKm;
    }

    private final Map<Long, Broadcast> store = new HashMap<>();
    private final Map<Long, DriverMatchedEvent> matchedOutbox = new HashMap<>();
    private final Set<Long> cancelledRides = new HashSet<>();

    @Override
    public synchronized boolean saveBroadcast(Long rideId, List<DriverOffer> offers,
                                           double radiusKm) {
        if (cancelledRides.contains(rideId)) return false;
        Broadcast broadcast = new Broadcast();
        broadcast.radiusKm = radiusKm;
        for (DriverOffer offer : offers) {
            broadcast.offers.put(offer.getDriverId(), copy(offer));
        }
        store.put(rideId, broadcast);
        return true;
    }

    @Override
    public synchronized CancelOutcome cancelPendingRide(Long rideId) {
        Broadcast broadcast = store.get(rideId);
        if (broadcast != null && broadcast.status == DriverOfferStatus.ACCEPTED) {
            return CancelOutcome.ALREADY_ACCEPTED;
        }
        cancelledRides.add(rideId);
        if (broadcast != null) broadcast.status = DriverOfferStatus.CANCELLED;
        return CancelOutcome.CANCELLED;
    }

    @Override
    public synchronized void recordCancellation(Long rideId) {
        cancelledRides.add(rideId);
        Broadcast broadcast = store.get(rideId);
        if (broadcast != null) broadcast.status = DriverOfferStatus.CANCELLED;
        matchedOutbox.remove(rideId);
    }

    @Override
    public synchronized boolean isRideCancelled(Long rideId) {
        return cancelledRides.contains(rideId);
    }

    @Override
    public synchronized Set<Long> findOfferedDriverIds(Long rideId) {
        Broadcast broadcast = store.get(rideId);
        return broadcast == null
                ? Set.of()
                : new HashSet<>(broadcast.offers.keySet());
    }

    @Override
    public synchronized Optional<DriverOffer> findOffer(Long rideId, Long driverId) {
        Broadcast broadcast = store.get(rideId);
        if (broadcast == null) {
            return Optional.empty();
        }
        DriverOffer offer = broadcast.offers.get(driverId);
        if (offer == null) {
            return Optional.empty();
        }
        DriverOffer copy = copy(offer);
        copy.setStatus(broadcast.status);
        return Optional.of(copy);
    }

    @Override
    public synchronized boolean tryAccept(Long rideId, Long driverId,
                                          DriverMatchedEvent matchedEvent) {
        Broadcast broadcast = store.get(rideId);
        if (broadcast == null
                || cancelledRides.contains(rideId)
                || broadcast.status != DriverOfferStatus.PENDING
                || !broadcast.offers.containsKey(driverId)
                || broadcast.declined.contains(driverId)
                || !broadcast.offers.get(driverId).getExpiresAt().isAfter(java.time.Instant.now())) {
            return false;
        }
        broadcast.status = DriverOfferStatus.ACCEPTED;
        broadcast.winner = String.valueOf(driverId);
        matchedOutbox.put(rideId, matchedEvent);
        return true;
    }

    @Override
    public synchronized List<DriverMatchedEvent> pendingMatchedEvents(int limit) {
        return matchedOutbox.values().stream().limit(limit).toList();
    }

    @Override
    public synchronized void acknowledgeMatchedEvent(Long rideId) {
        matchedOutbox.remove(rideId);
    }

    @Override
    public synchronized DeclineOutcome recordDecline(Long rideId, Long driverId) {
        Broadcast broadcast = store.get(rideId);
        if (broadcast == null
                || broadcast.status != DriverOfferStatus.PENDING
                || !broadcast.offers.containsKey(driverId)) {
            return DeclineOutcome.NOT_APPLICABLE;
        }
        broadcast.declined.add(driverId);
        return broadcast.declined.size() >= broadcast.offers.size()
                ? DeclineOutcome.ALL_DECLINED
                : DeclineOutcome.RECORDED;
    }

    @Override
    public synchronized boolean compareAndSetStatus(Long rideId,
                                                    DriverOfferStatus expected,
                                                    DriverOfferStatus next) {
        Broadcast broadcast = store.get(rideId);
        if (broadcast == null || broadcast.status != expected) {
            return false;
        }
        broadcast.status = next;
        return true;
    }

    @Override
    public synchronized boolean expireIfDue(Long rideId, java.time.Instant now) {
        Broadcast broadcast = store.get(rideId);
        if (broadcast == null || broadcast.status != DriverOfferStatus.PENDING
                || broadcast.offers.values().stream().noneMatch(
                    offer -> !offer.getExpiresAt().isAfter(now))) {
            return false;
        }
        broadcast.status = DriverOfferStatus.EXPIRED;
        return true;
    }

    @Override
    public synchronized void delete(Long rideId) {
        store.remove(rideId);
    }

    private DriverOffer copy(DriverOffer offer) {
        return DriverOffer.builder()
                .rideId(offer.getRideId())
                .driverId(offer.getDriverId())
                .status(offer.getStatus())
                .attempt(offer.getAttempt())
                .createdAt(offer.getCreatedAt())
                .expiresAt(offer.getExpiresAt())
                .build();
    }
}
