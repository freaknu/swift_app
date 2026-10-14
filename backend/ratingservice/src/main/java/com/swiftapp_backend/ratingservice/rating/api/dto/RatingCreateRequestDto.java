package com.swiftapp_backend.ratingservice.rating.api.dto;

import com.swiftapp_backend.ratingservice.rating.domain.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RatingCreateRequestDto {
    private Long riderId;
    private Long driverId;
    private UserType userType;
}