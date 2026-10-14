package com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.mapper;

import com.swiftapp_backend.matchingservice.matching.domain.models.RideRequest;
import com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.entity.RideRequestEntity;

public class RidePersistenceMapper {
    private RidePersistenceMapper(){}

    public static RideRequestEntity toEntity(RideRequest data) {
        return RideRequestEntity
                .builder()
                .latitude(data.getLatitude()).longitude(data.getLongitude())
                .radiusInKms(data.getRadiusInKms())
                .vehicleTypeEnum(data.getVehicleTypeEnum())
                .build();
    }

    public static RideRequest toDomain(RideRequestEntity data) {
        return RideRequest
                .builder()
                .latitude(data.getLatitude()).longitude(data.getLongitude())
                .radiusInKms(data.getRadiusInKms())
                .vehicleTypeEnum(data.getVehicleTypeEnum())
                .build();
    }
}