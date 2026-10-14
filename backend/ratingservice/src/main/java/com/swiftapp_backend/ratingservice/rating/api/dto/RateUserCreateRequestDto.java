package com.swiftapp_backend.ratingservice.rating.api.dto;

import com.swiftapp_backend.ratingservice.rating.domain.enums.UserType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RateUserCreateRequestDto {
    @NotNull @Positive
    private Long rideId;
    private Long riderId;
    private Long driverId;
    private String email;
    private String name;
    private String imageUrl;
    @NotNull @Min(1) @Max(5)
    private Long rate;
    private Long ratingId;
    @NotNull
    private UserType userType;
}
