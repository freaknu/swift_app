package com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.mapper;

import com.swiftapp_backend.matchingservice.matching.domain.models.DriverLocation;
import com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.entity.DriverLocationEntity;

public final class DriverLocationPersistenceMapper {

    private DriverLocationPersistenceMapper() {
    }

    public static DriverLocation toDomain(
            DriverLocationEntity data
    ) {

        return DriverLocation.builder()
                .driverId(data.getDriverId())
                .latitude(data.getLatitude())
                .longitude(data.getLongitude())
                .statusEvent(data.getDriverStatus())
                .vehicleType(data.getVehicleType())
                .build();
    }

    public static DriverLocationEntity toEntity(
            DriverLocation data
    ) {

        return DriverLocationEntity.builder()
                .driverId(data.getDriverId())
                .latitude(data.getLatitude())
                .longitude(data.getLongitude())
                .driverStatus(data.getStatusEvent())
                .vehicleType(data.getVehicleType())
                .build();
    }
}