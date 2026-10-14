package com.swiftapp_backend.ratingservice.rating.api.dto;

import com.swiftapp_backend.ratingservice.rating.api.dto.RateUserCreateResponseDto;
import com.swiftapp_backend.ratingservice.rating.domain.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RatingCreateResponseDto {
    private Long id;
    private Long riderId;
    private Long driverId;
    private List<RateUserCreateResponseDto> allRatings;
    private double averageRating;
    private UserType userType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}