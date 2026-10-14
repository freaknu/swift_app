package com.swiftapp_backend.ratingservice.rating.domain.model;

import com.swiftapp_backend.ratingservice.rating.domain.enums.UserType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class RatingModel {
    private Long id;

    private Long riderId;

    private Long driverId;

    private double averageRating;

    private UserType userType;

    private Boolean isActive;

    private List<RateUserModel> allRatings;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
