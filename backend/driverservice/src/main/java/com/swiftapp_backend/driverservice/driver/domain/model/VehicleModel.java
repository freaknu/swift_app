package com.swiftapp_backend.driverservice.driver.domain.model;

import com.swiftapp_backend.driverservice.driver.domain.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class VehicleModel {
    private Long id;

    private DriverModel driver;

    private VehicleType vehicleType;

    private Long authUserId;

    private String brand;
    private String model;
    private int year;
    private String registrationNumber;
    private String fuelType;
    private String color;
    private int seatCapacity;
    private boolean isActive;
    private boolean isEv;
    private int manufactureYear;
    private int seats;
    private Boolean isVerified;
    private List<String> vehicleImage;
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
