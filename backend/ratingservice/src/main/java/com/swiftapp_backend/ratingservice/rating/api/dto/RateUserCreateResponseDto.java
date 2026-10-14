package com.swiftapp_backend.ratingservice.rating.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RateUserCreateResponseDto {
    private Long id;
    private Long userId;
    private Long rideId;
    private double rate;
    private String email;
    private String name;
    private String imageUrl;
    private Long ratingId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
