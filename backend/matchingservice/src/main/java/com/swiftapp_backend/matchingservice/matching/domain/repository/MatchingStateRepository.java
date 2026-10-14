package com.swiftapp_backend.matchingservice.matching.domain.repository;

import com.swiftapp_backend.matchingservice.matching.domain.models.MatchingState;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideMatchFailedEvent;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Persistence for the per-ride matching state plus the attempted-drivers set.
 */
public interface MatchingStateRepository {

    void save(MatchingState state);

    /** Atomically stores terminal state and its pending rider notification. */
    /** False if a rider cancellation fenced this failure first. */
    boolean saveFailed(MatchingState state, RideMatchFailedEvent event);

    List<RideMatchFailedEvent> pendingFailedEvents(int limit);

    void acknowledgeFailedEvent(Long rideId);

    Optional<MatchingState> findByRideId(Long rideId);

    void delete(Long rideId);

    void addAttemptedDriver(Long rideId, Long driverId);

    Set<Long> attemptedDrivers(Long rideId);
}
