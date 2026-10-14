package com.swiftapp_backend.matchingservice.matching.domain.repository;

import java.time.Instant;
import java.util.List;

/**
 * Offer-expiry schedule backed by a Redis sorted set
 * (score = expiry epoch millis, member = rideId).
 */
public interface MatchingTimeoutRepository {

    void scheduleTimeout(Long rideId, Instant expiresAt);

    void cancelTimeout(Long rideId);

    List<Long> dueTimeouts(long nowEpochMilli, long limit);

    /** Claims one expiry for a short window so workers do not process it twice. */
    boolean claimTimeout(Long rideId);
}
