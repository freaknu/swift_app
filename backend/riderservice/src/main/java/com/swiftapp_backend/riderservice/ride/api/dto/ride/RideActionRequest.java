package com.swiftapp_backend.riderservice.ride.api.dto.ride;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RideActionRequest(@NotNull @Positive Long driverId) {
}
