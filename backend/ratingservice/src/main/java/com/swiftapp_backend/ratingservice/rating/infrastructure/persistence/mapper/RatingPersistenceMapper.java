package com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.mapper;

import com.swiftapp_backend.ratingservice.rating.domain.model.RateUserModel;
import com.swiftapp_backend.ratingservice.rating.domain.model.RatingModel;
import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.entity.RateUserEntity;
import com.swiftapp_backend.ratingservice.rating.infrastructure.persistence.entity.RatingEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RatingPersistenceMapper {
    private final RateUserPersistenceMapper rateUserMapper;

    public RatingModel toDomain(RatingEntity entity) {
        RatingModel model = toDomainReference(entity);
        if (model == null) {
            return null;
        }

        if (entity.getAllRatings() != null) {
            List<RateUserModel> ratings = new ArrayList<>();
            for (RateUserEntity rateEntity : entity.getAllRatings()) {
                RateUserModel rate = rateUserMapper.toDomain(rateEntity);
                rate.setRating(model);
                ratings.add(rate);
            }
            model.setAllRatings(ratings);
        }

        return model;
    }

    public RatingModel toDomainReference(RatingEntity entity) {
        if (entity == null) {
            return null;
        }

        return RatingModel.builder()
                .id(entity.getId())
                .riderId(entity.getRiderId())
                .driverId(entity.getDriverId())
                .averageRating(entity.getAverageRating())
                .userType(entity.getUserType())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public RatingEntity toEntity(RatingModel model) {
        RatingEntity entity = toEntityReference(model);
        if (entity == null) {
            return null;
        }

        if (model.getAllRatings() != null) {
            List<RateUserEntity> ratings = new ArrayList<>();
            for (RateUserModel rateModel : model.getAllRatings()) {
                RateUserEntity rate = rateUserMapper.toEntity(rateModel);
                rate.setRating(entity);
                ratings.add(rate);
            }
            entity.setAllRatings(ratings);
        }

        return entity;
    }

    public RatingEntity toEntityReference(RatingModel model) {
        if (model == null) {
            return null;
        }

        return RatingEntity.builder()
                .id(model.getId())
                .riderId(model.getRiderId())
                .driverId(model.getDriverId())
                .averageRating(model.getAverageRating())
                .userType(model.getUserType())
                .isActive(model.getIsActive())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
