package com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper;

import com.swiftapp_backend.userservice.user.domain.model.RideHistoryModel;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.RideHistoryEntity;
import org.springframework.stereotype.Component;

@Component
public class RideHistoryPersistenceMapper {

    public RideHistoryModel toDomain(RideHistoryEntity entity) {
        if (entity == null) {
            return null;
        }

        return RideHistoryModel.builder()
                .id(entity.getId())
                .authUserId(entity.getAuthUserId())
                .riderProfileId(entity.getRiderProfileId())
                .rideId(entity.getRideId())
                .pickUpLatitude(entity.getPickUpLatitude())
                .pickUpLongitude(entity.getPickUpLongitude())
                .destinationLatitude(entity.getDestinationLatitude())
                .destinationLongitude(entity.getDestinationLongitude())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public RideHistoryEntity toEntity(RideHistoryModel model) {
        if (model == null) {
            return null;
        }

        return RideHistoryEntity.builder()
                .id(model.getId())
                .authUserId(model.getAuthUserId())
                .riderProfileId(model.getRiderProfileId())
                .rideId(model.getRideId())
                .pickUpLatitude(model.getPickUpLatitude())
                .pickUpLongitude(model.getPickUpLongitude())
                .destinationLatitude(model.getDestinationLatitude())
                .destinationLongitude(model.getDestinationLongitude())
                .createdAt(model.getCreatedAt())
                .build();
    }
}
