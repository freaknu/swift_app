package com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper;

import com.swiftapp_backend.userservice.user.domain.model.AppliedOfferTrackModel;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.AppliedOfferTrackEntity;
import org.springframework.stereotype.Component;

@Component
public class AppliedOfferTrackPersistenceMapper {

    public AppliedOfferTrackModel toDomain(AppliedOfferTrackEntity entity) {
        if (entity == null) {
            return null;
        }

        return AppliedOfferTrackModel.builder()
                .id(entity.getId())
                .offerId(entity.getOfferId())
                .authUserId(entity.getAuthUserId())
                .riderId(entity.getRiderId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public AppliedOfferTrackEntity toEntity(AppliedOfferTrackModel model) {
        if (model == null) {
            return null;
        }

        return AppliedOfferTrackEntity.builder()
                .id(model.getId())
                .offerId(model.getOfferId())
                .authUserId(model.getAuthUserId())
                .riderId(model.getRiderId())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
