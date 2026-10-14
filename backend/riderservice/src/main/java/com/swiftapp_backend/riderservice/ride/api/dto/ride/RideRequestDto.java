package com.swiftapp_backend.riderservice.ride.api.dto.ride;

import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareEstimateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.NonNull;

public record RideRequestDto(
        @NonNull
        @Positive
        Long authUserId,
        Long offerId,
        @NotNull
        @Positive
        Long riderId,
        @NotBlank
        String rideType,
        @NotNull
        @Valid
        FareEstimateRequest route

) {
}
