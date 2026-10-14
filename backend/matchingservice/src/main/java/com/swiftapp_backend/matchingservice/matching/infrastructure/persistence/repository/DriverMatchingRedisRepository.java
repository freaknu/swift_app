package com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DriverMatchingRedisRepository {

    private static final String DRIVER_GEO_KEY = "drivers:geo";
    private static final String DRIVER_MATCHING_KEY_PREFIX =
            "driver-matching";
    private static final String DRIVER_LAST_SEEN_KEY = "drivers:last-seen";

    private final StringRedisTemplate redisTemplate;

    private final RedisTemplate<String, Object> redisPairTemplate;

    private final ObjectMapper objectMapper;

    public void updateDriverLocation(
            DriverLocationEntity location
    ) {

        if (location == null || location.getDriverId() == null) {
            log.warn("Cannot update driver location because driver data is null");
            return;
        }

        Point point = new Point(
                location.getLongitude(),
                location.getLatitude()
        );
        String driverId = String.valueOf(location.getDriverId());
        redisTemplate.opsForGeo().add(
                DRIVER_GEO_KEY,
                point,
                driverId
        );

        String key =
                DRIVER_MATCHING_KEY_PREFIX
                        + location.getDriverId();

        redisPairTemplate.opsForValue().set(
                key,
                location
        );

        redisTemplate.opsForHash().put(
                DRIVER_LAST_SEEN_KEY,
                driverId,
                String.valueOf(System.currentTimeMillis())
        );

        log.debug(
                "Driver Location Saved ========= {}",
                location
        );
    }

    public List<DriverLocationEntity> findAllNearbyDriver(
            double latitude,
            double longitude,
            VehicleTypeEnum vehicleType,
            double radiusInKilometers,
            long limit
    ) {

        Distance radius = new Distance(
                radiusInKilometers,
                Metrics.KILOMETERS
        );

        Circle searchArea = new Circle(
                new Point(
                        longitude,
                        latitude
                ),
                radius
        );

        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs
                        .newGeoRadiusArgs()
                        .includeCoordinates()
                        .includeDistance()
                        .sortAscending()
                        .limit(limit);
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(
                        DRIVER_GEO_KEY,
                        searchArea,
                        args
                );

        if (results == null
                || results.getContent() == null
                || results.getContent().isEmpty()) {

            return List.of();
        }

        List<String> driverKeys = new ArrayList<>();

        List<Long> driverIds = new ArrayList<>();

        for (
                GeoResult<RedisGeoCommands.GeoLocation<String>> result
                : results
        ) {

            RedisGeoCommands.GeoLocation<String> geoLocation =
                    result.getContent();

            if (geoLocation == null) {
                continue;
            }

            String driverIdString =
                    geoLocation.getName();

            if (driverIdString == null) {
                continue;
            }

            Long driverId;

            try {

                driverId =
                        Long.valueOf(driverIdString);

            } catch (NumberFormatException exception) {

                log.warn(
                        "Invalid driver ID found in Redis GEO: {}",
                        driverIdString
                );

                continue;
            }

            String driverKey =
                    DRIVER_MATCHING_KEY_PREFIX
                            + driverId;

            driverKeys.add(driverKey);
            driverIds.add(driverId);
        }

        if (driverKeys.isEmpty()) {
            return List.of();
        }

        List<Object> driverObjects =
                redisPairTemplate.opsForValue()
                        .multiGet(driverKeys);

        if (driverObjects == null
                || driverObjects.isEmpty()) {

            return List.of();
        }

        List<DriverLocationEntity> drivers =
                new ArrayList<>();

        for (Object object : driverObjects) {
            if (object == null) {
                continue;
            }

            DriverLocationEntity data;

            try {

                data = objectMapper.convertValue(
                        object,
                        DriverLocationEntity.class
                );

            } catch (IllegalArgumentException exception) {

                log.warn(
                        "Unable to convert Redis driver data: {}",
                        object,
                        exception
                );

                continue;
            }

            if (data == null) {
                continue;
            }

            if (data.getDriverStatus()
                    != DriverStatusEventEnum.ACTIVE) {

                continue;
            }

            if (vehicleType != null
                    && data.getVehicleType() != vehicleType) {

                continue;
            }


            drivers.add(data);
        }

        log.debug(
                "Nearby eligible drivers found = {}",
                drivers.size()
        );

        return drivers;
    }

    public void updateDriverStatus(
            Long driverId,
            DriverStatusEventEnum driverStatus
    ) {

        if (driverId == null || driverStatus == null) {
            return;
        }

        String key =
                DRIVER_MATCHING_KEY_PREFIX
                        + driverId;

        Object object =
                redisPairTemplate.opsForValue().get(key);

        if (object == null) {
            return;
        }

        DriverLocationEntity entity;

        try {

            entity = objectMapper.convertValue(
                    object,
                    DriverLocationEntity.class
            );

        } catch (IllegalArgumentException exception) {

            log.warn(
                    "Unable to convert Redis driver data for status update: {}",
                    object,
                    exception
            );

            return;
        }

        if (entity == null) {
            return;
        }

        entity.setDriverStatus(driverStatus);

        redisPairTemplate.opsForValue().set(
                key,
                entity
        );

        log.debug(
                "Driver status updated = {} -> {}",
                driverId,
                driverStatus
        );
    }
}
