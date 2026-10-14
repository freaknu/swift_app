package com.swiftapp_backend.matchingservice.matching.domain.models;

import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A driver eligible for a ride, as returned by the Redis GEO search.
 * Carries the distance so selection strategies can order candidates
 * without re-querying Redis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverCandidate {

    private Long driverId;

    private double latitude;

    private double longitude;

    /** Distance from the pickup point in kilometres (from GEOSEARCH). */
    private double distanceKm;

    private DriverStatusEventEnum status;

    private VehicleTypeEnum vehicleType;

    /** Epoch millis of the last location update, for staleness filtering. */
    private long lastSeenEpochMilli;
}
