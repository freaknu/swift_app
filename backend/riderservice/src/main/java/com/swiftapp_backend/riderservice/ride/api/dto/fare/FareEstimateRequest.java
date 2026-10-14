package com.swiftapp_backend.riderservice.ride.api.dto.fare;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FareEstimateRequest(
        Long offerId,
        @NotNull
        @DecimalMin("-90.0") @DecimalMax("90.0")
        Double pickupLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double
        pickupLongitude,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
        Double destinationLatitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double
        destinationLongitude,
        /**
         * Optional canonical vehicle type (e.g. BIKE, AUTO_RICKSHAW, HATCHBACK).
         * Legacy marketing codes remain accepted for existing clients.
         */
        String rideType
) {
}
