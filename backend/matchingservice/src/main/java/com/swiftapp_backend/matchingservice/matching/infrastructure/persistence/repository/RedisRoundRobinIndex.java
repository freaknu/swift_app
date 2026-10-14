package com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.repository;

import com.swiftapp_backend.matchingservice.matching.application.strategy.RoundRobinIndex;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed round-robin cursor shared by all instances.
 */
@Component
@RequiredArgsConstructor
public class RedisRoundRobinIndex implements RoundRobinIndex {

    private static final String KEY = "matching:round-robin:index";

    private final StringRedisTemplate redisTemplate;

    @Override
    public long nextIndex() {
        Long value = redisTemplate.opsForValue().increment(KEY);
        return value != null ? value : 0L;
    }
}
