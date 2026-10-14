package com.swiftapp_backend.matchingservice.matching.domain.dto;

import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Context passed to selection strategies for one matching attempt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchingContext {

    private Long rideId;

    private Long riderId;

    private double latitude;

    private double longitude;

    private VehicleTypeEnum vehicleType;

    private double radiusKm;

    private int attempt;

    private Set<Long> excludedDriverIds;
}
