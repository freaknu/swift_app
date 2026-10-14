package com.swiftapp_backend.riderservice.ride.api.dto.ride;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AcceptRideRequest(
        @NotNull @Positive Long driverId,
        @NotNull @Positive Long vehicleId
) {
}
