package com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.repository;

import com.swiftapp_backend.matchingservice.matching.domain.models.MatchingState;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideMatchFailedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.repository.MatchingStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Matching state: JSON at {@code ride:matching:{rideId}} plus the
 * attempted-drivers set at {@code ride:attempted-drivers:{rideId}}.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MatchingStateRedisRepository implements MatchingStateRepository {

    private static final String STATE_PREFIX = "ride:matching:";
    private static final String CANCELLED_PREFIX = "ride:cancelled:";

    private static final String ATTEMPTED_PREFIX = "ride:attempted-drivers:";

    private static final String FAILED_OUTBOX_HASH = "matching:outbox:match-failed:payloads";

    private static final String FAILED_OUTBOX_SET = "matching:outbox:match-failed:pending";

    private static final String SAVE_FAILED_SCRIPT =
            "if redis.call('exists', KEYS[4]) == 1 then return 0 end; "
                    + "redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[3]); "
                    + "redis.call('hset', KEYS[2], ARGV[4], ARGV[2]); "
                    + "redis.call('sadd', KEYS[3], ARGV[4]); return 1";

    private static final String SAVE_SCRIPT =
            "if redis.call('exists', KEYS[2]) == 1 and ARGV[3] ~= 'CANCELLED' "
                    + "then return 0 end; "
                    + "redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[2]); return 1";

    private static final String ACK_FAILED_SCRIPT =
            "redis.call('hdel', KEYS[1], ARGV[1]); "
                    + "redis.call('srem', KEYS[2], ARGV[1]); return 1";

    private static final Duration KEY_TTL = Duration.ofHours(2);

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public void save(MatchingState state) {
        try {
            String key = STATE_PREFIX + state.getRideId();
            redisTemplate.execute(new DefaultRedisScript<>(SAVE_SCRIPT, Long.class),
                    List.of(key, CANCELLED_PREFIX + state.getRideId()),
                    objectMapper.writeValueAsString(state),
                    String.valueOf(KEY_TTL.toSeconds()), state.getStatus().name());
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to save matching state for ride " + state.getRideId(), ex);
        }
    }

    @Override
    public boolean saveFailed(MatchingState state, RideMatchFailedEvent event) {
        try {
            Long result = redisTemplate.execute(new DefaultRedisScript<>(SAVE_FAILED_SCRIPT, Long.class),
                    List.of(STATE_PREFIX + state.getRideId(), FAILED_OUTBOX_HASH,
                            FAILED_OUTBOX_SET, CANCELLED_PREFIX + state.getRideId()),
                    objectMapper.writeValueAsString(state),
                    objectMapper.writeValueAsString(event),
                    String.valueOf(KEY_TTL.toSeconds()),
                    String.valueOf(state.getRideId()));
            return result != null && result == 1L;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to store matching failure for ride " + state.getRideId(), ex);
        }
    }

    @Override
    public List<RideMatchFailedEvent> pendingFailedEvents(int limit) {
        Set<String> ids = redisTemplate.opsForSet().members(FAILED_OUTBOX_SET);
        if (ids == null || ids.isEmpty()) return List.of();
        List<RideMatchFailedEvent> pending = new ArrayList<>();
        for (String id : ids) {
            if (pending.size() >= limit) break;
            Object json = redisTemplate.opsForHash().get(FAILED_OUTBOX_HASH, id);
            if (json == null) continue;
            try {
                pending.add(objectMapper.readValue(json.toString(), RideMatchFailedEvent.class));
            } catch (Exception ex) {
                log.error("Corrupt ride.match.failed outbox payload for ride {}", id, ex);
            }
        }
        return pending;
    }

    @Override
    public void acknowledgeFailedEvent(Long rideId) {
        redisTemplate.execute(new DefaultRedisScript<>(ACK_FAILED_SCRIPT, Long.class),
                List.of(FAILED_OUTBOX_HASH, FAILED_OUTBOX_SET), String.valueOf(rideId));
    }

    @Override
    public Optional<MatchingState> findByRideId(Long rideId) {
        String json = redisTemplate.opsForValue().get(STATE_PREFIX + rideId);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, MatchingState.class));
        } catch (Exception ex) {
            log.warn("Corrupt matching state for ride {}", rideId, ex);
            return Optional.empty();
        }
    }

    @Override
    public void delete(Long rideId) {
        redisTemplate.delete(STATE_PREFIX + rideId);
        redisTemplate.delete(ATTEMPTED_PREFIX + rideId);
    }

    @Override
    public void addAttemptedDriver(Long rideId, Long driverId) {
        String key = ATTEMPTED_PREFIX + rideId;
        redisTemplate.opsForSet().add(key, String.valueOf(driverId));
        redisTemplate.expire(key, KEY_TTL);
    }

    @Override
    public Set<Long> attemptedDrivers(Long rideId) {
        Set<String> members =
                redisTemplate.opsForSet().members(ATTEMPTED_PREFIX + rideId);
        if (members == null) {
            return Set.of();
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
                .collect(Collectors.toSet());
    }
}
