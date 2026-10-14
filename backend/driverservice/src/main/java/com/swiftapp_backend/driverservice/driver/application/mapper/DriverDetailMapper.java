package com.swiftapp_backend.driverservice.driver.application.mapper;


import com.swiftapp_backend.driverservice.driver.api.dto.DriverCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverResponseDto;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@Slf4j
public class DriverDetailMapper {
    public static DriverModel toModel(DriverCreateRequestDto dto) {
        return DriverModel
                .builder()
                .driverName(dto.getDriverName())
                .email(dto.getEmail())
                .driverAvailability(dto.getDriverAvailability())
                .driverPreferences(dto.getDriverPreferences())
                .driverStatus(dto.getDriverStatus())
                .gender(dto.getGender())
                .isAcceptingRides(dto.getIsAcceptingRides())
                .languages(dto.getLanguages())
                .phoneNumber(dto.getPhoneNumber())
                .profileImage(dto.getProfileImage())
                .dateOfBirth(dto.getDateOfBirth())
                .build();
    }

    public static DriverResponseDto toResponseDto(DriverModel model) {
        try {
            return DriverResponseDto
                    .builder()
                    .driverId(model.getId())
                    .driverName(model.getDriverName())
                    .email(model.getEmail())
                    .driverPreferences(model.getDriverPreferences())
                    .gender(model.getGender())
                    .dateOfBirth(model.getDateOfBirth())
                    .isAcceptingRides(model.isAcceptingRides())
                    .driverStatus(model.getDriverStatus())
                    .isActive(model.isActive())
                    .driverAvailability(model.getDriverAvailability())
                    .languages(model.getLanguages())
                    .phoneNumber(model.getPhoneNumber())
                    .profileImage(model.getProfileImage())
                    .allVehicles(model.getAllVehicles() == null
                            ? List.of()
                            : model.getAllVehicles().stream()
                            .map(VehicleMapper::toVehicleResponse)
                            .toList())
                    .allDocuments(model.getAllDocuments() == null
                            ? List.of()
                            : model.getAllDocuments().stream()
                            .filter(document -> document.isActive())
                            .map(DriverDocumentMapper::toResponse)
                            .toList())
                    .build();
        }
        catch (Exception e) {
            log.error("The error is {}",e);
            throw e;
        }

    }
}
