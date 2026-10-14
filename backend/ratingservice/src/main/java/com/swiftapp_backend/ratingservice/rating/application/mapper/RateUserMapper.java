package com.swiftapp_backend.ratingservice.rating.application.mapper;

import com.swiftapp_backend.ratingservice.rating.api.dto.RateUserCreateRequestDto;
import com.swiftapp_backend.ratingservice.rating.api.dto.RateUserCreateResponseDto;
import com.swiftapp_backend.ratingservice.rating.domain.model.RateUserModel;
import org.springframework.stereotype.Component;

@Component
public class RateUserMapper {

    public RateUserModel toModel(RateUserCreateRequestDto data) {

        Long userId = data.getDriverId() != null
                ? data.getDriverId()
                : data.getRiderId();

        return RateUserModel.builder()
                .userId(userId)
                .name(data.getName())
                .email(data.getEmail())
                .imageUrl(data.getImageUrl())
                .rate(data.getRate())
                .build();
    }

    public RateUserCreateResponseDto toDto(RateUserModel data) {

        return RateUserCreateResponseDto.builder()
                .id(data.getId())
                // The aggregate may be viewed by its recipient. Do not expose
                // another participant's profile identity or email address.
                .rate(data.getRate())
                .rideId(data.getRideId())
                .ratingId(data.getRating() != null ? data.getRating().getId() : null)
                .createdAt(data.getCreatedAt())
                .updatedAt(data.getUpdatedAt())
                .build();
    }
}
