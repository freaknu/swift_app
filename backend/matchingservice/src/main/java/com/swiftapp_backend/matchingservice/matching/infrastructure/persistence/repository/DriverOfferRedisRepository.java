package com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.repository;

import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverOfferStatus;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverOffer;
import com.swiftapp_backend.matchingservice.matching.domain.events.DriverMatchedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverOfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Broadcast offer storage.
 *
 * <ul>
 *   <li>Hash {@code ride:offer:{rideId}}: ride-level {@code status},
 *       {@code winner}, {@code attempt}, {@code radiusKm},
 *       {@code expiresAt}, {@code driverCount} and per-driver
 *       {@code payload:{driverId}} JSON.</li>
 *   <li>Set {@code ride:offer:{rideId}:drivers}: targeted driver ids.</li>
 *   <li>Set {@code ride:offer:{rideId}:declined}: drivers who declined.</li>
 * </ul>
 *
 * <p>Status transitions go through Lua compare-and-set so the drivers'
 * accepts and the timeout worker are mutually exclusive: the first
 * accept wins.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DriverOfferRedisRepository implements DriverOfferRepository {

    private static final String OFFER_PREFIX = "ride:offer:";
    private static final String DRIVERS_SUFFIX = ":drivers";
    private static final String DECLINED_SUFFIX = ":declined";
    private static final String CANCELLED_PREFIX = "ride:cancelled:";

    private static final String FIELD_STATUS = "status";
    private static final String FIELD_WINNER = "winner";
    private static final String FIELD_ATTEMPT = "attempt";
    private static final String FIELD_RADIUS_KM = "radiusKm";
    private static final String FIELD_EXPIRES_AT = "expiresAt";
    private static final String FIELD_EXPIRES_AT_EPOCH = "expiresAtEpoch";
    private static final String FIELD_DRIVER_COUNT = "driverCount";
    private static final String FIELD_PAYLOAD_PREFIX = "payload:";
    private static final String MATCHED_OUTBOX_HASH = "matching:outbox:driver-matched:payloads";
    private static final String MATCHED_OUTBOX_SET = "matching:outbox:driver-matched:pending";

    private static final Duration KEY_TTL = Duration.ofHours(2);

    private static final String SAVE_BROADCAST_SCRIPT =
            "if redis.call('exists', KEYS[4]) == 1 then return 0 end "
                    + "redis.call('del', KEYS[1], KEYS[2], KEYS[3]); "
                    + "redis.call('hset', KEYS[1], 'status', ARGV[1], 'winner', '', "
                    + "'attempt', ARGV[2], 'radiusKm', ARGV[3], 'expiresAt', ARGV[4], "
                    + "'expiresAtEpoch', ARGV[5], 'driverCount', ARGV[6]); "
                    + "for i=8,#ARGV,2 do "
                    + "redis.call('hset', KEYS[1], 'payload:' .. ARGV[i], ARGV[i+1]); "
                    + "redis.call('sadd', KEYS[2], ARGV[i]); end; "
                    + "redis.call('expire', KEYS[1], ARGV[7]); "
                    + "redis.call('expire', KEYS[2], ARGV[7]); return 1";

    private static final String CANCEL_PENDING_SCRIPT =
            "local status=redis.call('hget', KEYS[1], 'status'); "
                    + "if status == 'ACCEPTED' then return 2 end; "
                    + "redis.call('set', KEYS[2], '1', 'EX', ARGV[1]); "
                    + "if status then redis.call('hset', KEYS[1], 'status', 'CANCELLED') end; "
                    + "return 1";

    private static final String RECORD_CANCELLATION_SCRIPT =
            "redis.call('set', KEYS[2], '1', 'EX', ARGV[1]); "
                    + "if redis.call('exists', KEYS[1]) == 1 then "
                    + "redis.call('hset', KEYS[1], 'status', 'CANCELLED') end; "
                    + "redis.call('hdel', KEYS[3], ARGV[2]); "
                    + "redis.call('srem', KEYS[4], ARGV[2]); return 1";

    private static final String ACCEPT_SCRIPT =
            "if redis.call('exists', KEYS[6]) == 0 "
                    + "and redis.call('hget', KEYS[1], 'status') == ARGV[1] "
                    + "and redis.call('sismember', KEYS[2], ARGV[2]) == 1 "
                    + "and redis.call('sismember', KEYS[3], ARGV[2]) == 0 "
                    + "and tonumber(redis.call('hget', KEYS[1], 'expiresAtEpoch') or '0') > tonumber(ARGV[4]) then "
                    + "  redis.call('hset', KEYS[1], 'status', ARGV[3], 'winner', ARGV[2]) "
                    + "  redis.call('hset', KEYS[4], ARGV[6], ARGV[5]) "
                    + "  redis.call('sadd', KEYS[5], ARGV[6]) "
                    + "  return 1 "
                    + "else "
                    + "  return 0 "
                    + "end";

    private static final String DECLINE_SCRIPT =
            "if redis.call('hget', KEYS[1], 'status') ~= ARGV[1] then "
                    + "  return 0 "
                    + "end "
                    + "if tonumber(redis.call('hget', KEYS[1], 'expiresAtEpoch') or '0') <= tonumber(ARGV[4]) then "
                    + "  return 0 "
                    + "end "
                    + "if redis.call('sismember', KEYS[2], ARGV[2]) == 0 then "
                    + "  return 0 "
                    + "end "
                    + "redis.call('sadd', KEYS[3], ARGV[2]) "
                    + "if redis.call('scard', KEYS[3]) >= tonumber(ARGV[3]) then "
                    + "  return 2 "
                    + "else "
                    + "  return 1 "
                    + "end";

    private static final String CAS_SCRIPT =
            "if redis.call('hget', KEYS[1], 'status') == ARGV[1] then "
                    + "  redis.call('hset', KEYS[1], 'status', ARGV[2]) "
                    + "  return 1 "
                    + "else "
                    + "  return 0 "
                    + "end";

    private static final String EXPIRE_IF_DUE_SCRIPT =
            "if redis.call('hget', KEYS[1], 'status') == ARGV[1] "
                    + "and tonumber(redis.call('hget', KEYS[1], 'expiresAtEpoch') or '0') <= tonumber(ARGV[3]) then "
                    + "  redis.call('hset', KEYS[1], 'status', ARGV[2]) "
                    + "  return 1 "
                    + "else return 0 end";

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    @Override
    public boolean saveBroadcast(Long rideId, List<DriverOffer> offers, double radiusKm) {
        if (offers == null || offers.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot save an empty broadcast for ride " + rideId);
        }
        try {
            DriverOffer first = offers.get(0);
            List<String> args = new ArrayList<>(List.of(
                    DriverOfferStatus.PENDING.name(),
                    String.valueOf(first.getAttempt()),
                    String.valueOf(radiusKm),
                    first.getExpiresAt() != null ? first.getExpiresAt().toString() : "",
                    first.getExpiresAt() != null
                            ? String.valueOf(first.getExpiresAt().toEpochMilli()) : "0",
                    String.valueOf(offers.size()),
                    String.valueOf(KEY_TTL.toSeconds())));
            for (DriverOffer offer : offers) {
                args.add(String.valueOf(offer.getDriverId()));
                args.add(objectMapper.writeValueAsString(offer));
            }
            Long result = redisTemplate.execute(
                    new DefaultRedisScript<>(SAVE_BROADCAST_SCRIPT, Long.class),
                    List.of(key(rideId), driversKey(rideId), declinedKey(rideId),
                            cancelledKey(rideId)),
                    args.toArray(String[]::new));
            return result != null && result == 1L;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to save broadcast for ride " + rideId, ex);
        }
    }

    @Override
    public CancelOutcome cancelPendingRide(Long rideId) {
        Long result = redisTemplate.execute(
                new DefaultRedisScript<>(CANCEL_PENDING_SCRIPT, Long.class),
                List.of(key(rideId), cancelledKey(rideId)),
                String.valueOf(KEY_TTL.toSeconds()));
        if (result == null) throw new IllegalStateException("Could not cancel offer for ride " + rideId);
        return result == 2L ? CancelOutcome.ALREADY_ACCEPTED : CancelOutcome.CANCELLED;
    }

    @Override
    public void recordCancellation(Long rideId) {
        redisTemplate.execute(new DefaultRedisScript<>(RECORD_CANCELLATION_SCRIPT, Long.class),
                List.of(key(rideId), cancelledKey(rideId), MATCHED_OUTBOX_HASH,
                        MATCHED_OUTBOX_SET),
                String.valueOf(KEY_TTL.toSeconds()), String.valueOf(rideId));
    }

    @Override
    public boolean isRideCancelled(Long rideId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(cancelledKey(rideId)));
    }

    @Override
    public Set<Long> findOfferedDriverIds(Long rideId) {
        Set<String> members = redisTemplate.opsForSet().members(driversKey(rideId));
        if (members == null || members.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> ids = new HashSet<>();
        for (String member : members) {
            try {
                ids.add(Long.valueOf(member));
            } catch (NumberFormatException ignored) {
                log.warn("Corrupt driver id '{}' in offer set for ride {}", member, rideId);
            }
        }
        return ids;
    }

    @Override
    public Optional<DriverOffer> findOffer(Long rideId, Long driverId) {
        Object payload = redisTemplate.opsForHash()
                .get(key(rideId), FIELD_PAYLOAD_PREFIX + driverId);
        if (payload == null) {
            return Optional.empty();
        }
        try {
            DriverOffer offer =
                    objectMapper.readValue(payload.toString(), DriverOffer.class);
            Object status = redisTemplate.opsForHash().get(key(rideId), FIELD_STATUS);
            if (status != null) {
                // The hash field is authoritative for the ride-level status.
                offer.setStatus(DriverOfferStatus.valueOf(status.toString()));
            }
            return Optional.of(offer);
        } catch (Exception ex) {
            log.warn("Corrupt offer data for ride {} driver {}", rideId, driverId, ex);
            return Optional.empty();
        }
    }

    @Override
    public boolean tryAccept(Long rideId, Long driverId, DriverMatchedEvent matchedEvent) {
        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>(ACCEPT_SCRIPT, Long.class);
        try {
            String eventJson = objectMapper.writeValueAsString(matchedEvent);
            Long result = redisTemplate.execute(script,
                    List.of(key(rideId), driversKey(rideId), declinedKey(rideId),
                            MATCHED_OUTBOX_HASH, MATCHED_OUTBOX_SET,
                            cancelledKey(rideId)),
                    DriverOfferStatus.PENDING.name(),
                    String.valueOf(driverId),
                    DriverOfferStatus.ACCEPTED.name(),
                    String.valueOf(System.currentTimeMillis()),
                    eventJson,
                    String.valueOf(rideId));
            return result != null && result == 1L;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not record matched event for ride " + rideId, ex);
        }
    }

    @Override
    public List<DriverMatchedEvent> pendingMatchedEvents(int limit) {
        Set<String> ids = redisTemplate.opsForSet().members(MATCHED_OUTBOX_SET);
        if (ids == null || ids.isEmpty()) return List.of();
        List<DriverMatchedEvent> pending = new java.util.ArrayList<>();
        for (String id : ids) {
            if (pending.size() >= limit) break;
            Object json = redisTemplate.opsForHash().get(MATCHED_OUTBOX_HASH, id);
            if (json == null) continue;
            try {
                pending.add(objectMapper.readValue(json.toString(), DriverMatchedEvent.class));
            } catch (Exception ex) {
                log.error("Corrupt driver.matched outbox payload for ride {}", id, ex);
            }
        }
        return pending;
    }

    @Override
    public void acknowledgeMatchedEvent(Long rideId) {
        String id = String.valueOf(rideId);
        String ackScript = "redis.call('hdel', KEYS[1], ARGV[1]); "
                + "redis.call('srem', KEYS[2], ARGV[1]); return 1";
        redisTemplate.execute(new DefaultRedisScript<>(ackScript, Long.class),
                List.of(MATCHED_OUTBOX_HASH, MATCHED_OUTBOX_SET), id);
    }

    @Override
    public DeclineOutcome recordDecline(Long rideId, Long driverId) {
        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>(DECLINE_SCRIPT, Long.class);
        Object count = redisTemplate.opsForHash().get(key(rideId), FIELD_DRIVER_COUNT);
        String driverCount = count != null ? count.toString() : "0";
        Long result = redisTemplate.execute(script,
                List.of(key(rideId), driversKey(rideId), declinedKey(rideId)),
                DriverOfferStatus.PENDING.name(),
                String.valueOf(driverId),
                driverCount,
                String.valueOf(System.currentTimeMillis()));
        if (result == null || result == 0L) {
            return DeclineOutcome.NOT_APPLICABLE;
        }
        return result == 2L ? DeclineOutcome.ALL_DECLINED : DeclineOutcome.RECORDED;
    }

    @Override
    public boolean compareAndSetStatus(Long rideId,
                                       DriverOfferStatus expected,
                                       DriverOfferStatus next) {
        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>(CAS_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script,
                List.of(key(rideId)), expected.name(), next.name());
        return result != null && result == 1L;
    }

    @Override
    public boolean expireIfDue(Long rideId, java.time.Instant now) {
        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>(EXPIRE_IF_DUE_SCRIPT, Long.class);
        Long result = redisTemplate.execute(script, List.of(key(rideId)),
                DriverOfferStatus.PENDING.name(), DriverOfferStatus.EXPIRED.name(),
                String.valueOf(now.toEpochMilli()));
        return result != null && result == 1L;
    }

    @Override
    public void delete(Long rideId) {
        redisTemplate.delete(List.of(key(rideId), driversKey(rideId), declinedKey(rideId)));
    }

    private String key(Long rideId) {
        return OFFER_PREFIX + rideId;
    }

    private String driversKey(Long rideId) {
        return OFFER_PREFIX + rideId + DRIVERS_SUFFIX;
    }

    private String declinedKey(Long rideId) {
        return OFFER_PREFIX + rideId + DECLINED_SUFFIX;
    }

    private String cancelledKey(Long rideId) {
        return CANCELLED_PREFIX + rideId;
    }
}
