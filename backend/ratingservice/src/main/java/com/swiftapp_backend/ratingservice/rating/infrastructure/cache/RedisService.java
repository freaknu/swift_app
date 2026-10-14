package com.swiftapp_backend.ratingservice.rating.infrastructure.cache;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.ratingservice.rating.application.port.RatingCache;
import com.swiftapp_backend.ratingservice.rating.domain.model.RateUserModel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisService implements RatingCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<List<RateUserModel>> findRatings(Long ratingId) {
        String key = "ratings" + ratingId;
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            return Optional.empty();
        }

        List<Object> values = getList(key);
        List<RateUserModel> ratings = objectMapper.convertValue(
                values,
                new TypeReference<>() {
                }
        );
        return Optional.of(ratings);
    }

    @Override
    public void storeRatings(Long ratingId, List<RateUserModel> ratings) {
        if (!ratings.isEmpty()) {
            redisTemplate.opsForList().rightPushAll("ratings" + ratingId, ratings);
        }
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

    public void delete(String key) {
        redisTemplate.delete(key);
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
