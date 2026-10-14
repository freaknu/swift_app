package com.swiftapp_backend.locationservice.location.infrastructure.persistence.mapper;

import com.swiftapp_backend.locationservice.location.domain.model.Location;
import com.swiftapp_backend.locationservice.location.infrastructure.persistence.entity.LocationEntity;

public final class LocationPersistenceMapper {
    private LocationPersistenceMapper() {
    }

    public static LocationEntity toEntity(Location location) {
        return LocationEntity.builder()
                .id(location.getId())
                .driverId(location.getDriverId())
                .rideId(location.getRideId())
                .riderId(location.getRiderId())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .nearByPlace(location.getNearByPlace())
                .direction(location.getDirection())
                .createdAt(location.getCreatedAt())
                .updatedAt(location.getUpdatedAt())
                .build();
    }

    public static Location toDomain(LocationEntity entity) {
        return Location.builder()
                .id(entity.getId())
                .driverId(entity.getDriverId())
                .rideId(entity.getRideId())
                .riderId(entity.getRiderId())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .nearByPlace(entity.getNearByPlace())
                .direction(entity.getDirection())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
