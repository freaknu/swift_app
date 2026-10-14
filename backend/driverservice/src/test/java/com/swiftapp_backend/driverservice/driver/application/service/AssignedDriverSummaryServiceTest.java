package com.swiftapp_backend.driverservice.driver.application.service;

import com.swiftapp_backend.driverservice.driver.domain.enums.VehicleType;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.VehicleRepository;
import com.swiftapp_backend.driverservice.shared.common.ApiException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssignedDriverSummaryServiceTest {
    private final DriverRepository drivers = mock(DriverRepository.class);
    private final VehicleRepository vehicles = mock(VehicleRepository.class);
    private final AssignedDriverSummaryService service =
            new AssignedDriverSummaryService(drivers, vehicles);

    @Test
    void returnsOnlyRealAssignedDriverAndVehicleSummary() {
        DriverModel driver = DriverModel.builder().id(55L).authUserId(42L)
                .driverName("Alex").profileImage("/images/alex.jpg").build();
        when(drivers.findById(55L)).thenReturn(Optional.of(driver));
        when(vehicles.findById(77L)).thenReturn(Optional.of(VehicleModel.builder()
                .id(77L).driver(driver).vehicleType(VehicleType.SEDAN)
                .brand("Honda").model("City").registrationNumber("KA01AB1234")
                .isVerified(true).build()));

        var result = service.get(55L, 77L);

        assertEquals("Alex", result.driverName());
        assertEquals("SEDAN", result.vehicleType());
        assertEquals("KA01AB1234", result.registrationNumber());
        assertEquals(Boolean.TRUE, result.vehicleVerified());
    }

    @Test
    void rejectsVehicleOwnedByAnotherDriver() {
        when(drivers.findById(55L)).thenReturn(Optional.of(
                DriverModel.builder().id(55L).authUserId(42L).build()));
        when(vehicles.findById(77L)).thenReturn(Optional.of(VehicleModel.builder()
                .id(77L).driver(DriverModel.builder().id(56L).build())
                .authUserId(43L).build()));

        assertThrows(ApiException.class, () -> service.get(55L, 77L));
    }
}
