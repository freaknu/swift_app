package com.swiftapp_backend.websocketservice.realtime.application.service;

import com.swiftapp_backend.websocketservice.realtime.domain.enums.RideStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DriverRideLightweightService {
    private static final String ACTIVE_RIDE_PREFIX = "driver-temp-ride-";
    private static final String ACTIVE_RIDE_ID_PREFIX = "driver-temp-ride-id-";
    private static final String PICKUP_LATITUDE_PREFIX = "ride-pickup-latitude-";
    private static final String PICKUP_LONGITUDE_PREFIX = "ride-pickup-longitude-";
    private static final String DESTINATION_LATITUDE_PREFIX = "ride-destination-latitude-";
    private static final String DESTINATION_LONGITUDE_PREFIX = "ride-destination-longitude-";
    private static final String RIDER_PREFIX = "ride-rider-";
    private static final String ARRIVAL_PREFIX = "ride-arrival-published-";
    private static final Duration CONTEXT_TTL = Duration.ofHours(24);
    private static final Set<RideStatus> LOCATION_STATUSES = EnumSet.of(
            RideStatus.DRIVER_ASSIGNED,
            RideStatus.DRIVER_ARRIVED,
            RideStatus.STARTED
    );

    private final RedisTemplate<String, Object> redisTemplate;

    /** Stores the assignment context received from rider-service. */
    public void putDriverHasActiveRide(Long driverId, Long rideId, RideStatus status,
                                       Double pickupLatitude, Double pickupLongitude,
                                       Long riderId) {
        putDriverHasActiveRide(driverId, rideId, status, pickupLatitude, pickupLongitude,
                null, null, riderId);
    }

    public void putDriverHasActiveRide(Long driverId, Long rideId, RideStatus status,
                                       Double pickupLatitude, Double pickupLongitude,
                                       Double destinationLatitude, Double destinationLongitude,
                                       Long riderId) {
        if (driverId == null || rideId == null || status == null) return;
        redisTemplate.opsForValue().set(statusKey(driverId), status.name(), CONTEXT_TTL);
        redisTemplate.opsForValue().set(rideKey(driverId), rideId, CONTEXT_TTL);
        if (pickupLatitude != null) {
            redisTemplate.opsForValue().set(pickupLatitudeKey(rideId), pickupLatitude, CONTEXT_TTL);
        }
        if (pickupLongitude != null) {
            redisTemplate.opsForValue().set(pickupLongitudeKey(rideId), pickupLongitude, CONTEXT_TTL);
        }
        if (destinationLatitude != null) {
            redisTemplate.opsForValue().set(destinationLatitudeKey(rideId), destinationLatitude, CONTEXT_TTL);
        }
        if (destinationLongitude != null) {
            redisTemplate.opsForValue().set(destinationLongitudeKey(rideId), destinationLongitude, CONTEXT_TTL);
        }
        if (riderId != null) {
            redisTemplate.opsForValue().set(riderKey(rideId), riderId, CONTEXT_TTL);
        }
    }

    /** Backwards-compatible helper for callers that only know the status. */
    public void putDriverHasActiveRide(Long driverId, RideStatus status) {
        if (driverId != null && status != null) {
            redisTemplate.opsForValue().set(statusKey(driverId), status.name(), CONTEXT_TTL);
        }
    }

    public RideStatus getDriverHasActiveRide(Long driverId) {
        if (driverId == null) return null;
        Object value = redisTemplate.opsForValue().get(statusKey(driverId));
        if (value == null) return null;
        try {
            return value instanceof RideStatus
                    ? (RideStatus) value
                    : RideStatus.valueOf(value.toString().replace("\"", ""));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public Long getActiveRideId(Long driverId) {
        if (driverId == null) return null;
        Object value = redisTemplate.opsForValue().get(rideKey(driverId));
        if (value instanceof Number number) return number.longValue();
        if (value != null) {
            try {
                return Long.valueOf(value.toString().replace("\"", ""));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public boolean acceptsLocation(Long driverId, Long rideId) {
        return rideId != null
                && Objects.equals(rideId, getActiveRideId(driverId))
                && LOCATION_STATUSES.contains(getDriverHasActiveRide(driverId));
    }

    public Double getPickupLatitude(Long rideId) {
        return getDouble(pickupLatitudeKey(rideId));
    }

    public Double getPickupLongitude(Long rideId) {
        return getDouble(pickupLongitudeKey(rideId));
    }

    public Double getDestinationLatitude(Long rideId) {
        return getDouble(destinationLatitudeKey(rideId));
    }

    public Double getDestinationLongitude(Long rideId) {
        return getDouble(destinationLongitudeKey(rideId));
    }

    public Long getRiderId(Long rideId) {
        Object value = redisTemplate.opsForValue().get(riderKey(rideId));
        if (value instanceof Number number) return number.longValue();
        if (value == null) return null;
        try {
            return Long.valueOf(value.toString().replace("\"", ""));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public void updateStatus(Long driverId, RideStatus status) {
        if (driverId != null && status != null) {
            redisTemplate.opsForValue().set(statusKey(driverId), status.name(), CONTEXT_TTL);
        }
    }

    /** Returns true only for the first arrival notification for a ride. */
    public boolean markArrivalPublished(Long rideId) {
        if (rideId == null) return false;
        Boolean created = redisTemplate.opsForValue().setIfAbsent(
                arrivalKey(rideId), "1", 24, TimeUnit.HOURS);
        return Boolean.TRUE.equals(created);
    }

    public void clear(Long driverId, Long rideId) {
        if (driverId != null) {
            redisTemplate.delete(statusKey(driverId));
            redisTemplate.delete(rideKey(driverId));
        }
        if (rideId != null) {
            redisTemplate.delete(pickupLatitudeKey(rideId));
            redisTemplate.delete(pickupLongitudeKey(rideId));
            redisTemplate.delete(destinationLatitudeKey(rideId));
            redisTemplate.delete(destinationLongitudeKey(rideId));
            redisTemplate.delete(riderKey(rideId));
            redisTemplate.delete(arrivalKey(rideId));
        }
    }

    private Double getDouble(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Number number) return number.doubleValue();
        if (value == null) return null;
        try {
            return Double.valueOf(value.toString().replace("\"", ""));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String statusKey(Long driverId) {
        return ACTIVE_RIDE_PREFIX + driverId;
    }

    private String rideKey(Long driverId) {
        return ACTIVE_RIDE_ID_PREFIX + driverId;
    }

    private String pickupLatitudeKey(Long rideId) {
        return PICKUP_LATITUDE_PREFIX + rideId;
    }

    private String pickupLongitudeKey(Long rideId) {
        return PICKUP_LONGITUDE_PREFIX + rideId;
    }

    private String destinationLatitudeKey(Long rideId) {
        return DESTINATION_LATITUDE_PREFIX + rideId;
    }

    private String destinationLongitudeKey(Long rideId) {
        return DESTINATION_LONGITUDE_PREFIX + rideId;
    }

    private String riderKey(Long rideId) {
        return RIDER_PREFIX + rideId;
    }

    private String arrivalKey(Long rideId) {
        return ARRIVAL_PREFIX + rideId;
    }
}
