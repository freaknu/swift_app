package com.swiftapp_backend.matchingservice.matching.support;

import com.swiftapp_backend.matchingservice.matching.domain.models.MatchingState;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideMatchFailedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.repository.MatchingStateRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** In-memory fake. */
public class InMemoryMatchingStateRepository implements MatchingStateRepository {

    private final Map<Long, MatchingState> states = new HashMap<>();
    private final Map<Long, RideMatchFailedEvent> failedEvents = new HashMap<>();

    private final Map<Long, Set<Long>> attempted = new HashMap<>();

    @Override
    public synchronized void save(MatchingState state) {
        states.put(state.getRideId(), state);
    }

    @Override
    public synchronized boolean saveFailed(MatchingState state, RideMatchFailedEvent event) {
        states.put(state.getRideId(), state);
        failedEvents.put(state.getRideId(), event);
        return true;
    }

    @Override
    public synchronized List<RideMatchFailedEvent> pendingFailedEvents(int limit) {
        return failedEvents.values().stream().limit(limit).toList();
    }

    @Override
    public synchronized void acknowledgeFailedEvent(Long rideId) {
        failedEvents.remove(rideId);
    }

    @Override
    public synchronized Optional<MatchingState> findByRideId(Long rideId) {
        return Optional.ofNullable(states.get(rideId));
    }

    @Override
    public synchronized void delete(Long rideId) {
        states.remove(rideId);
        attempted.remove(rideId);
    }

    @Override
    public synchronized void addAttemptedDriver(Long rideId, Long driverId) {
        attempted.computeIfAbsent(rideId, k -> new HashSet<>()).add(driverId);
    }

    @Override
    public synchronized Set<Long> attemptedDrivers(Long rideId) {
        return Set.copyOf(attempted.getOrDefault(rideId, Set.of()));
    }
}
