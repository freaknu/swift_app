package com.swiftapp_backend.driverservice.driver.api.dto;

import com.swiftapp_backend.driverservice.driver.api.dto.DriverDocumentResponseDto;
import com.swiftapp_backend.driverservice.driver.api.dto.VehicleResponseDto;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverAvailability;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverPreferences;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverStatus;
import com.swiftapp_backend.driverservice.driver.domain.enums.Gender;
import com.swiftapp_backend.driverservice.driver.domain.enums.Language;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponseDto {

    private Long driverId;

    private String driverName;

    private String email;

    private String phoneNumber;

    private Gender gender;

    private String profileImage;

    private DriverStatus driverStatus;

    private boolean isActive;

    private boolean isAcceptingRides;

    private DriverAvailability driverAvailability;

    private List<DriverPreferences> driverPreferences;

    private List<VehicleResponseDto> allVehicles;

    private LocalDate dateOfBirth;

    private List<Language> languages;

    private List<DriverDocumentResponseDto> allDocuments;
}
