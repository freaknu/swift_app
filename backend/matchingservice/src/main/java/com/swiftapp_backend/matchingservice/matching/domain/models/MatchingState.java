package com.swiftapp_backend.matchingservice.matching.domain.models;

import com.swiftapp_backend.matchingservice.matching.domain.enums.MatchingStatus;
import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.math.BigDecimal;

/**
 * Durable state of one ride's matching lifecycle, stored in Redis.
 * Keeps everything the timeout worker and reassignment need
 * (pickup point, vehicle type, attempt count) so they only need the rideId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingState {

    private Long rideId;

    private Long riderId;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private Double destinationLatitude;

    private Double destinationLongitude;

    private Double estimatedDistance;

    private Long estimatedDuration;

    private BigDecimal totalFare;

    private String currency;

    private VehicleTypeEnum vehicleType;

    private MatchingStatus status;

    private Long driverId;

    private int attempt;

    /**
     * Search radius (km) used by the latest broadcast attempt.
     * Shown to the rider as "searching within X km".
     */
    private Double radiusKm;

    private Instant createdAt;

    private Instant updatedAt;

    public boolean isTerminal() {
        return status == MatchingStatus.MATCHED
                || status == MatchingStatus.FAILED
                || status == MatchingStatus.CANCELLED;
    }
}
