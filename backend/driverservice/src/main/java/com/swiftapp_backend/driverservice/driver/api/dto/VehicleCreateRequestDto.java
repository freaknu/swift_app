package com.swiftapp_backend.driverservice.driver.api.dto;

import com.swiftapp_backend.driverservice.driver.domain.enums.VehicleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCreateRequestDto {

        @NotNull(message = "Vehicle type is required")
        private VehicleType vehicleType;

        @NotBlank(message = "Brand is required")
        private String brand;

        @NotBlank(message = "Model is required")
        private String model;

        @NotNull(message = "Year is required")
        @Min(value = 1980, message = "Invalid vehicle year")
        @Max(value = 2100, message = "Invalid vehicle year")
        private Integer year;

        @NotBlank(message = "Registration number is required")
        private String registrationNumber;

        private String fuelType;

        private String color;

        @Min(value = 1, message = "Seat capacity must be at least 1")
        private Integer seatCapacity;

        private Boolean isEv;

        @Min(value = 1980)
        @Max(value = 2100)
        private Integer manufactureYear;

        @Min(value = 1)
        private Integer seats;

        private List<String> vehicleImage;
}
