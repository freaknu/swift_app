package com.swiftapp_backend.driverservice.driver.domain.model;

import com.swiftapp_backend.driverservice.driver.domain.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverModel {
    private Long id;

    private Long authUserId;

    private String driverName;

    private String email;

    private String phoneNumber;

    private Gender gender;

    private String profileImage;

    private boolean isActive;

    private DriverStatus driverStatus;

    private boolean isAcceptingRides;

    private DriverAvailability driverAvailability;

    private boolean isVerified;
    private List<DriverPreferences> driverPreferences;

    private List<VehicleModel> allVehicles;

    private LocalDate dateOfBirth;

    private List<Language> languages;

    private List<DriverDocumentModel> allDocuments;
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
