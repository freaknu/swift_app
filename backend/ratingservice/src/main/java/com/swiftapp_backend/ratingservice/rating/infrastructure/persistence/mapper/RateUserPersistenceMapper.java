package com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.mapper;

import com.swiftapp_backend.ratingservice.rating.domain.model.RateUserModel;
import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.entity.RateUserEntity;
import org.springframework.stereotype.Component;

@Component
public class RateUserPersistenceMapper {

    public RateUserModel toDomain(RateUserEntity entity) {
        if (entity == null) {
            return null;
        }

        return RateUserModel.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .raterAuthUserId(entity.getRaterAuthUserId())
                .rideId(entity.getRideId())
                .email(entity.getEmail())
                .name(entity.getName())
                .imageUrl(entity.getImageUrl())
                .rate(entity.getRate())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public RateUserEntity toEntity(RateUserModel model) {
        if (model == null) {
            return null;
        }

        return RateUserEntity.builder()
                .id(model.getId())
                .userId(model.getUserId())
                .raterAuthUserId(model.getRaterAuthUserId())
                .rideId(model.getRideId())
                .email(model.getEmail())
                .name(model.getName())
                .imageUrl(model.getImageUrl())
                .rate(model.getRate())
                .isActive(model.getIsActive())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
