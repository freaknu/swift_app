package com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.repository;

import com.swiftapp_backend.matchingservice.matching.domain.repository.MatchingTimeoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.Duration;
import java.util.List;
import java.util.Set;

/**
 * Offer-expiry schedule: sorted set {@code matching:timeouts},
 * score = expiry epoch millis, member = rideId.
 */
@Repository
@RequiredArgsConstructor
public class MatchingTimeoutRedisRepository implements MatchingTimeoutRepository {

    private static final String TIMEOUTS_KEY = "matching:timeouts";
    private static final String TIMEOUT_CLAIM_PREFIX = "matching:timeout-claim:";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void scheduleTimeout(Long rideId, Instant expiresAt) {
        redisTemplate.opsForZSet().add(
                TIMEOUTS_KEY, String.valueOf(rideId), expiresAt.toEpochMilli());
    }

    @Override
    public void cancelTimeout(Long rideId) {
        redisTemplate.opsForZSet().remove(TIMEOUTS_KEY, String.valueOf(rideId));
    }

    @Override
    public List<Long> dueTimeouts(long nowEpochMilli, long limit) {
        Set<String> members = redisTemplate.opsForZSet()
                .rangeByScore(TIMEOUTS_KEY, 0, nowEpochMilli, 0, limit);
        if (members == null) {
            return List.of();
        }
        return members.stream()
                .map(value -> {
                    try {
                        return Long.valueOf(value);
                    } catch (NumberFormatException ex) {
                        return null;
                    }
                })
                .filter(id -> id != null)
                .toList();
    }

    @Override
    public boolean claimTimeout(Long rideId) {
        Boolean claimed = redisTemplate.opsForValue().setIfAbsent(
                TIMEOUT_CLAIM_PREFIX + rideId, "1", Duration.ofSeconds(5));
        return Boolean.TRUE.equals(claimed);
    }
}
