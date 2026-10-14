package com.swiftapp_backend.authservice.authentication.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.authservice.authentication.application.port.OtpStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService implements OtpStore {

    // Each state transition is atomic with resend and expiry. All arguments use the same
    // value serializer as the OTP, so comparisons also work with the existing integer codes.
    private static final DefaultRedisScript<Long> STORE_OTP = new DefaultRedisScript<>("""
            redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2])
            redis.call('DEL', KEYS[2], KEYS[3], KEYS[4])
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> VERIFY_OTP = new DefaultRedisScript<>("""
            local code = redis.call('GET', KEYS[1])
            local ttl = redis.call('PTTL', KEYS[1])
            if not code or code ~= ARGV[1] or ttl <= 0 or redis.call('EXISTS', KEYS[3]) == 1 then
                return 0
            end
            redis.call('SET', KEYS[2], code, 'PX', ttl)
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> CLAIM_OTP = new DefaultRedisScript<>("""
            local code = redis.call('GET', KEYS[1])
            local ttl = redis.call('PTTL', KEYS[1])
            if not code or ttl <= 0 or redis.call('GET', KEYS[2]) ~= code
                or redis.call('EXISTS', KEYS[3]) == 1 then
                return 0
            end
            if ARGV[1] == 'true' and code ~= ARGV[2] then return 0 end
            redis.call('SET', KEYS[3], ARGV[3], 'PX', ttl)
            redis.call('SET', KEYS[4], ARGV[4], 'PX', ttl)
            return 1
            """, Long.class);
    private static final DefaultRedisScript<Long> COMPLETE_OTP = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[3]) == ARGV[1] and redis.call('GET', KEYS[4]) == ARGV[2] then
                return redis.call('DEL', KEYS[1], KEYS[2], KEYS[3], KEYS[4])
            end
            return 0
            """, Long.class);
    private static final DefaultRedisScript<Long> CONSUME_OTP = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] and redis.call('EXISTS', KEYS[3]) == 0 then
                return redis.call('DEL', KEYS[1], KEYS[2], KEYS[3], KEYS[4])
            end
            return 0
            """, Long.class);
    private static final DefaultRedisScript<Long> RELEASE_OTP = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[3]) == ARGV[1] and redis.call('GET', KEYS[4]) == ARGV[2] then
                return redis.call('DEL', KEYS[3], KEYS[4])
            end
            return 0
            """, Long.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void store(String key, int otp, Duration ttl) {
        redisTemplate.execute(STORE_OTP, otpKeys(key), otp, ttl.toMillis());
    }

    @Override
    public Optional<Integer> find(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null
                ? Optional.empty()
                : Optional.of(objectMapper.convertValue(value, Integer.class));
    }

    @Override
    public boolean verify(String key, int otp) {
        return Long.valueOf(1).equals(redisTemplate.execute(VERIFY_OTP, otpKeys(key), otp));
    }

    @Override
    public boolean claimVerified(String key, Integer otp, String completionToken, String sessionHash) {
        return Long.valueOf(1).equals(redisTemplate.execute(
                CLAIM_OTP, otpKeys(key), otp != null, otp == null ? 0 : otp, completionToken, sessionHash));
    }

    @Override
    public void complete(String key, String completionToken, String sessionHash) {
        redisTemplate.execute(COMPLETE_OTP, otpKeys(key), completionToken, sessionHash);
    }

    @Override
    public void release(String key, String completionToken, String sessionHash) {
        redisTemplate.execute(RELEASE_OTP, otpKeys(key), completionToken, sessionHash);
    }

    @Override
    public boolean consume(String key, int otp) {
        Long deleted = redisTemplate.execute(CONSUME_OTP, otpKeys(key), otp);
        return deleted != null && deleted > 0;
    }

    private static List<String> otpKeys(String key) {
        return List.of(key, key + ":verified", key + ":completion", key + ":session");
    }


    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long ttl, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, ttl, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public boolean exists(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(otpKeys(key));
    }


    public void expire(String key, long ttl, TimeUnit unit) {
        redisTemplate.expire(key, ttl, unit);
    }

    public Long getRemainingTtl(String key) {
        return redisTemplate.getExpire(key);
    }


    public void addToList(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
    }

    public void addAllToList(String key, List<?> values) {
        redisTemplate.opsForList().rightPushAll(key, values);
    }

    public List<Object> getList(String key) {
        Long size = redisTemplate.opsForList().size(key);

        if (size == null || size == 0) {
            return List.of();
        }

        return redisTemplate.opsForList()
                .range(key, 0, size - 1);
    }

    public Object popFromList(String key) {
        return redisTemplate.opsForList().leftPop(key);
    }


    public void addToSet(String key, Object value) {
        redisTemplate.opsForSet().add(key, value);
    }

    public boolean isMember(String key, Object value) {
        Boolean member = redisTemplate.opsForSet()
                .isMember(key, value);

        return Boolean.TRUE.equals(member);
    }


    public void putHashValue(
            String key,
            String field,
            Object value
    ) {
        redisTemplate.opsForHash()
                .put(key, field, value);
    }

    public Object getHashValue(
            String key,
            String field
    ) {
        return redisTemplate.opsForHash()
                .get(key, field);
    }

    public void deleteHashField(
            String key,
            String field
    ) {
        redisTemplate.opsForHash()
                .delete(key, field);
    }
}
