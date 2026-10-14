package com.swiftapp_backend.driverservice.driver.application.mapper;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DriverDetailMapperAvailabilityTest {
    @Test
    void profileExposesActualAccountActiveFlagToLocationService() {
        assertTrue(DriverDetailMapper.toResponseDto(
                DriverModel.builder().id(7L).isActive(true).build()).isActive());
        assertFalse(DriverDetailMapper.toResponseDto(
                DriverModel.builder().id(7L).isActive(false).build()).isActive());
    }
}
