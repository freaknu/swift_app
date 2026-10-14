package com.swiftapp_backend.locationservice.eta.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.locationservice.eta.application.port.out.EtaCachePort;
import com.swiftapp_backend.locationservice.eta.application.port.out.RouteResult;
import com.swiftapp_backend.locationservice.eta.infrastructure.routing.google.GoogleRoutesMapper;
import com.swiftapp_backend.locationservice.eta.domain.model.RouteCoordinates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisEtaCacheAdapter implements EtaCachePort {
    private static final String KEY_PREFIX = "eta:route:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${eta.cache-ttl-seconds:10}")
    private long ttlSeconds;

    @Override
    public Optional<RouteResult> find(RouteCoordinates coordinates, String rideType) {
        try {
            Object cached = redisTemplate.opsForValue().get(key(coordinates, rideType));
            if (cached == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.convertValue(cached, RouteResult.class));
        } catch (DataAccessException | IllegalArgumentException ex) {
            log.debug("ETA cache read failed; calculating route directly", ex);
            return Optional.empty();
        }
    }

    @Override
    public void save(RouteCoordinates coordinates, String rideType, RouteResult result) {
        try {
            redisTemplate.opsForValue().set(key(coordinates, rideType), result,
                    Math.max(1, ttlSeconds), TimeUnit.SECONDS);
        } catch (DataAccessException ex) {
            log.debug("ETA cache write failed", ex);
        }
    }

    private String key(RouteCoordinates coordinates, String rideType) {
        return String.format(Locale.ROOT, "%s%.6f:%.6f:%.6f:%.6f:%s",
                KEY_PREFIX,
                coordinates.originLatitude(), coordinates.originLongitude(),
                coordinates.destinationLatitude(), coordinates.destinationLongitude(),
                GoogleRoutesMapper.toTravelMode(rideType));
    }
}
