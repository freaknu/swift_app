package com.swiftapp_backend.driverservice.driver.application.service;

import com.swiftapp_backend.driverservice.driver.api.dto.AssignedDriverSummaryDto;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import com.swiftapp_backend.driverservice.driver.domain.repository.VehicleRepository;
import com.swiftapp_backend.driverservice.shared.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AssignedDriverSummaryService {
    private final DriverRepository drivers;
    private final VehicleRepository vehicles;

    public AssignedDriverSummaryDto get(Long driverId, Long vehicleId) {
        DriverModel driver = drivers.findById(driverId)
                .orElseThrow(() -> ApiException.notFound("Driver not found"));
        VehicleModel vehicle = vehicles.findById(vehicleId)
                .orElseThrow(() -> ApiException.notFound("Vehicle not found"));
        Long vehicleDriverId = vehicle.getDriver() == null ? null : vehicle.getDriver().getId();
        if (!driverId.equals(vehicleDriverId)
                && (driver.getAuthUserId() == null
                    || !Objects.equals(driver.getAuthUserId(), vehicle.getAuthUserId()))) {
            throw ApiException.notFound("Vehicle not assigned to driver");
        }
        return new AssignedDriverSummaryDto(driverId, driver.getDriverName(),
                driver.getProfileImage(), vehicleId,
                vehicle.getVehicleType() == null ? null : vehicle.getVehicleType().name(),
                vehicle.getBrand(), vehicle.getModel(), vehicle.getColor(),
                vehicle.getRegistrationNumber(), vehicle.getSeatCapacity(), vehicle.getIsVerified());
    }
}
