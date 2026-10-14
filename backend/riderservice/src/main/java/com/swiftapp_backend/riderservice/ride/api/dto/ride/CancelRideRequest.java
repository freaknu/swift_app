package com.swiftapp_backend.riderservice.ride.api.dto.ride;

import com.swiftapp_backend.riderservice.ride.domain.enums.ChangedByType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CancelRideRequest(
        @NotNull @Positive Long actorId,
        @NotNull ChangedByType actorType,
        @NotBlank @Size(max = 300) String reason
) {
}
