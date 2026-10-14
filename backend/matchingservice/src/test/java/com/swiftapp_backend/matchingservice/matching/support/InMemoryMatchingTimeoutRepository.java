package com.swiftapp_backend.matchingservice.matching.support;

import com.swiftapp_backend.matchingservice.matching.domain.repository.MatchingTimeoutRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** In-memory fake with real due-timeout semantics. */
public class InMemoryMatchingTimeoutRepository implements MatchingTimeoutRepository {

    private final Map<Long, Instant> timeouts = new HashMap<>();
    private final Map<Long, Instant> claims = new HashMap<>();

    @Override
    public synchronized void scheduleTimeout(Long rideId, Instant expiresAt) {
        timeouts.put(rideId, expiresAt);
    }

    @Override
    public synchronized void cancelTimeout(Long rideId) {
        timeouts.remove(rideId);
    }

    @Override
    public synchronized List<Long> dueTimeouts(long nowEpochMilli, long limit) {
        return timeouts.entrySet().stream()
                .filter(e -> e.getValue().toEpochMilli() <= nowEpochMilli)
                .sorted(Map.Entry.comparingByValue())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }

    @Override
    public synchronized boolean claimTimeout(Long rideId) {
        Instant now = Instant.now();
        Instant previous = claims.get(rideId);
        if (previous != null && previous.isAfter(now)) return false;
        claims.put(rideId, now.plusSeconds(5));
        return true;
    }

    /** Test helper. */
    public synchronized boolean hasTimeout(Long rideId) {
        return timeouts.containsKey(rideId);
    }
}
