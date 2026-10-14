package com.swiftapp_backend.driverservice.driver.api.dto;

import com.swiftapp_backend.driverservice.driver.domain.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponseDto {

    private Long id;

    private VehicleType vehicleType;

    private Long driverId;

    private Long authUserId;

    private String brand;

    private String model;

    private Integer year;

    private String registrationNumber;

    private String fuelType;

    private String color;

    private Integer seatCapacity;

    private boolean isActive;

    private Boolean isVerified;

    private boolean isEv;

    private Integer manufactureYear;

    private Integer seats;

    private List<String> vehicleImage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
