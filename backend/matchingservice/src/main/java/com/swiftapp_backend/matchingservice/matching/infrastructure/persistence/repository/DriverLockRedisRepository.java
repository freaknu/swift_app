package com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.repository;

import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Redis distributed lock: {@code driver:matching-lock:{driverId} = rideId}.
 * Acquire is SET NX EX (atomic). Release is a Lua compare-and-delete so a
 * stale timeout handler can never remove another ride's lock.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DriverLockRedisRepository implements DriverLockRepository {

    private static final String LOCK_PREFIX = "driver:matching-lock:";

    /**
     * Deletes the key only if its value still equals the given rideId.
     */
    private static final String RELEASE_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('del', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";

    private final StringRedisTemplate redisTemplate;

    @Override
    public boolean acquireLock(Long driverId, Long rideId, Duration ttl) {
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                key(driverId), String.valueOf(rideId), ttl);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public boolean releaseLock(Long driverId, Long rideId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(RELEASE_SCRIPT, Long.class);
        Long released = redisTemplate.execute(
                script, List.of(key(driverId)), String.valueOf(rideId));
        return released != null && released == 1L;
    }

    @Override
    public boolean isLocked(Long driverId) {
        Boolean exists = redisTemplate.hasKey(key(driverId));
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Optional<Long> lockedBy(Long driverId) {
        String value = redisTemplate.opsForValue().get(key(driverId));
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(value));
        } catch (NumberFormatException ex) {
            log.warn("Corrupt lock value for driver {}: {}", driverId, value);
            return Optional.empty();
        }
    }

    private String key(Long driverId) {
        return LOCK_PREFIX + driverId;
    }
}
