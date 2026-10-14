package com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.repository;

import com.swiftapp_backend.matchingservice.matching.application.config.MatchingProperties;
import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverCandidate;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverCandidateRepository;
import com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.entity.DriverLocationEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Nearby-driver search with distances. Unlike the legacy repository,
 * this keeps the GEO distance on every candidate and drops drivers
 * whose last location update is stale.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DriverCandidateRedisRepository implements DriverCandidateRepository {

    private static final String DRIVER_GEO_KEY = "drivers:geo";

    private static final String DRIVER_MATCHING_KEY_PREFIX = "driver-matching";

    private static final String DRIVER_LAST_SEEN_KEY = "drivers:last-seen";

    private final StringRedisTemplate redisTemplate;

    private final RedisTemplate<String, Object> redisPairTemplate;

    private final ObjectMapper objectMapper;

    private final MatchingProperties properties;

    @Override
    public List<DriverCandidate> findCandidates(double latitude,
                                                double longitude,
                                                VehicleTypeEnum vehicleType,
                                                double radiusKm,
                                                long limit) {
        Distance radius = new Distance(radiusKm, Metrics.KILOMETERS);
        Circle searchArea = new Circle(new Point(longitude, latitude), radius);

        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeCoordinates()
                        .includeDistance()
                        .sortAscending()
                        .limit(limit);

        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(DRIVER_GEO_KEY, searchArea, args);

        if (results == null || results.getContent() == null || results.getContent().isEmpty()) {
            return List.of();
        }

        List<String> driverKeys = new ArrayList<>();
        List<Double> distancesKm = new ArrayList<>();

        for (GeoResult<RedisGeoCommands.GeoLocation<String>> result : results) {
            RedisGeoCommands.GeoLocation<String> geoLocation = result.getContent();
            if (geoLocation == null || geoLocation.getName() == null) {
                continue;
            }
            Long driverId;
            try {
                driverId = Long.valueOf(geoLocation.getName());
            } catch (NumberFormatException ex) {
                log.warn("Invalid driver ID in Redis GEO: {}", geoLocation.getName());
                continue;
            }
            double distanceKm = result.getDistance() != null
                    ? result.getDistance().getValue()
                    : Double.MAX_VALUE;
            driverKeys.add(DRIVER_MATCHING_KEY_PREFIX + driverId);
            distancesKm.add(distanceKm);
        }

        if (driverKeys.isEmpty()) {
            return List.of();
        }

        List<Object> driverObjects =
                redisPairTemplate.opsForValue().multiGet(driverKeys);
        Map<String, String> lastSeenMap = readLastSeen();

        long now = System.currentTimeMillis();
        long staleAfterMs = properties.getStaleLocationThresholdSeconds() * 1000L;

        List<DriverCandidate> candidates = new ArrayList<>();
        for (int i = 0; i < driverKeys.size(); i++) {
            Object object = driverObjects != null && i < driverObjects.size()
                    ? driverObjects.get(i) : null;
            if (object == null) {
                continue;
            }
            DriverLocationEntity entity;
            try {
                entity = objectMapper.convertValue(object, DriverLocationEntity.class);
            } catch (IllegalArgumentException ex) {
                log.warn("Unable to convert Redis driver data: {}", object, ex);
                continue;
            }
            if (entity == null || entity.getDriverId() == null) {
                continue;
            }
            if (entity.getDriverStatus() != DriverStatusEventEnum.ACTIVE) {
                continue;
            }
            if (Boolean.TRUE.equals(redisTemplate.hasKey(
                    "driver:active-ride:" + entity.getDriverId()))) {
                continue;
            }
            if (vehicleType != null && entity.getVehicleType() != vehicleType) {
                continue;
            }

            String driverIdPart = driverKeys.get(i)
                    .substring(DRIVER_MATCHING_KEY_PREFIX.length());
            long lastSeen = parseLastSeen(lastSeenMap.get(driverIdPart));
            if (lastSeen <= 0 || now - lastSeen > staleAfterMs) {
                log.debug("Skipping stale driver {} (last seen {} ms ago)",
                        entity.getDriverId(), now - lastSeen);
                continue;
            }

            candidates.add(DriverCandidate.builder()
                    .driverId(entity.getDriverId())
                    .latitude(entity.getLatitude())
                    .longitude(entity.getLongitude())
                    .distanceKm(distancesKm.get(i))
                    .status(entity.getDriverStatus())
                    .vehicleType(entity.getVehicleType())
                    .lastSeenEpochMilli(lastSeen)
                    .build());
        }

        log.debug("Nearby eligible candidates found = {}", candidates.size());
        return candidates;
    }

    private Map<String, String> readLastSeen() {
        try {
            Map<String, String> entries =
                    redisTemplate.<String, String>opsForHash().entries(DRIVER_LAST_SEEN_KEY);
            return entries != null ? entries : Map.of();
        } catch (Exception ex) {
            log.debug("Could not read driver last-seen times", ex);
            return Map.of();
        }
    }

    private long parseLastSeen(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
