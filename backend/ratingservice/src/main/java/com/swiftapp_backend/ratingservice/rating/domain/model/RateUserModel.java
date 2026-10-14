package com.swiftapp_backend.ratingservice.rating.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class RateUserModel {
    private Long id;

    private Long userId;
    private Long raterAuthUserId;
    private Long rideId;

    private String email;
    private String name;
    private String imageUrl;
    private double rate;
    private Boolean isActive;
    private RatingModel rating;
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
