package com.swiftapp_backend.locationservice.location.infrastructure.cache;

import com.swiftapp_backend.locationservice.location.application.port.LocationCachePort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisLocationCacheAdapter implements LocationCachePort {

    private final RedisTemplate<String, Object> redisTemplate;


    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public void set(String key, Object value, long ttl, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, ttl, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public boolean exists(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    public Set<String> scanKeys(String pattern) {
        Set<String> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(1000)
                .build();

        redisTemplate.execute((RedisCallback<Void>) connection -> {
            try (Cursor<byte[]> cursor = connection.scan(options)) {
                while (cursor.hasNext()) {
                    keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                }
            }
            return null;
        });

        return keys;
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(key);
    }


    public void expire(String key, long ttl, TimeUnit unit) {
        redisTemplate.expire(key, ttl, unit);
    }

    public Long getRemainingTtl(String key) {
        return redisTemplate.getExpire(key);
    }


    @Override
    public void append(String key, Object value) {
        redisTemplate.opsForList().rightPush(key, value);
        redisTemplate.opsForList().trim(key, -200, -1);
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }

    public void addAllToList(String key, List<?> values) {
        redisTemplate.opsForList().rightPushAll(key, values);
    }

    @Override
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

    @Override
    public void addGeoLocation(
            String key,
            String member,
            double latitude,
            double longitude
    ) {
        redisTemplate.opsForGeo()
                .add(key, new Point(longitude, latitude), member);
    }

    @Override
    public void removeGeoLocation(String key, String member) {
        redisTemplate.opsForGeo()
                .remove(key, member);
    }

    @Override
    public List<String> findGeoMembersWithin(
            String key,
            double latitude,
            double longitude,
            double radiusKm
    ) {
        GeoResults<RedisGeoCommands.GeoLocation<Object>> results =
                redisTemplate.opsForGeo()
                        .radius(
                                key,
                                new Circle(
                                        new Point(longitude, latitude),
                                        new Distance(radiusKm, Metrics.KILOMETERS)
                                ),
                                RedisGeoCommands.GeoRadiusCommandArgs
                                        .newGeoRadiusArgs()
                                        .includeDistance()
                                        .sortAscending()
                        );

        if (results == null) {
            return List.of();
        }

        return results.getContent().stream()
                .map(result -> String.valueOf(result.getContent().getName()))
                .toList();
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
