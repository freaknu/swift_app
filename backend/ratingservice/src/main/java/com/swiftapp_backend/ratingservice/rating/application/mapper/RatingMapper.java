package com.swiftapp_backend.ratingservice.rating.application.mapper;

import com.swiftapp_backend.ratingservice.rating.api.dto.RatingCreateRequestDto;
import com.swiftapp_backend.ratingservice.rating.api.dto.RatingCreateResponseDto;
import com.swiftapp_backend.ratingservice.rating.domain.model.RatingModel;
import org.springframework.stereotype.Component;

@Component
public class RatingMapper {
    public RatingModel toModel(RatingCreateRequestDto data) {
        return RatingModel
                .builder()
                .userType(data.getUserType())
                .driverId(data.getDriverId())
                .riderId(data.getRiderId())
                .build();
    }

    public RatingCreateResponseDto toDto(RatingModel data) {
        return RatingCreateResponseDto
                .builder()
                .id(data.getId())
                .riderId(data.getRiderId())
                .driverId(data.getDriverId())
                .averageRating(data.getAverageRating())
                .userType(data.getUserType())
                .createdAt(data.getCreatedAt())
                .updatedAt(data.getUpdatedAt())
                .build();
    }
}