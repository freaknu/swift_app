package com.swiftapp_backend.riderservice.ride.api.dto.fare;

import lombok.Builder;

@Builder
public record FareEstimateDto(
        double estimatedDistanceKm,
        long estimatedDurationSeconds,
        FareDto fare
) {
}
