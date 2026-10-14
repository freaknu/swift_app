package com.swiftapp_backend.locationservice.eta.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record EtaRequest(
        @NotBlank(message = "rideId is required")
        String rideId,

        @NotBlank(message = "driverId is required")
        String driverId,

        @NotNull(message = "driverLatitude is required")
        @DecimalMin(value = "-90.0", message = "driverLatitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "driverLatitude must be between -90 and 90")
        Double driverLatitude,

        @NotNull(message = "driverLongitude is required")
        @DecimalMin(value = "-180.0", message = "driverLongitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "driverLongitude must be between -180 and 180")
        Double driverLongitude,

        @NotNull(message = "destinationLatitude is required")
        @DecimalMin(value = "-90.0", message = "destinationLatitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "destinationLatitude must be between -90 and 90")
        Double destinationLatitude,

        @NotNull(message = "destinationLongitude is required")
        @DecimalMin(value = "-180.0", message = "destinationLongitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "destinationLongitude must be between -180 and 180")
        Double destinationLongitude,

        String rideType
) {
}
