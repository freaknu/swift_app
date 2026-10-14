package com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.mapper;

import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverLocation;
import com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.entity.DriverLocationEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression test: the location pipeline must not drop the driver's status
 * or vehicle type. Dropping them made every Kafka-fed driver invisible to
 * the candidate filter (status != ACTIVE), so no offers were ever created.
 */
class DriverLocationPersistenceMapperTest {

    @Test
    void toEntityPreservesStatusAndVehicleType() {
        DriverLocation location = DriverLocation.builder()
                .driverId(102L)
                .latitude(12.9716)
                .longitude(77.5946)
                .statusEvent(DriverStatusEventEnum.ACTIVE)
                .vehicleType(VehicleTypeEnum.SEDAN)
                .build();

        DriverLocationEntity entity = DriverLocationPersistenceMapper.toEntity(location);

        assertEquals(102L, entity.getDriverId());
        assertEquals(DriverStatusEventEnum.ACTIVE, entity.getDriverStatus());
        assertEquals(VehicleTypeEnum.SEDAN, entity.getVehicleType());
    }

    @Test
    void toDomainRoundTripPreservesStatusAndVehicleType() {
        DriverLocationEntity entity = DriverLocationEntity.builder()
                .driverId(102L)
                .latitude(12.9716)
                .longitude(77.5946)
                .driverStatus(DriverStatusEventEnum.ACTIVE)
                .vehicleType(VehicleTypeEnum.SEDAN)
                .build();

        DriverLocation location = DriverLocationPersistenceMapper.toDomain(entity);

        assertEquals(DriverStatusEventEnum.ACTIVE, location.getStatusEvent());
        assertEquals(VehicleTypeEnum.SEDAN, location.getVehicleType());
    }
}
