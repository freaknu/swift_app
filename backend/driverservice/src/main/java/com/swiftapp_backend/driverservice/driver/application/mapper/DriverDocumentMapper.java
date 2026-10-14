package com.swiftapp_backend.driverservice.driver.application.mapper;


import com.swiftapp_backend.driverservice.driver.api.dto.DriverDocumentCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverDocumentResponseDto;
import com.swiftapp_backend.driverservice.driver.domain.enums.VerificationStatus;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverDocumentModel;


public class DriverDocumentMapper {
    public static DriverDocumentModel toModel(DriverDocumentCreateRequestDto data) {
        return DriverDocumentModel
                .builder()
                .documentType(data.getDocumentType())
                .documentUrl(data.getDocumentUrl())
                .verificationStatus(VerificationStatus.PENDING)
                .build();
    }

    public static DriverDocumentResponseDto toResponse(DriverDocumentModel model) {
        return DriverDocumentResponseDto.builder()
                .id(model.getId())
                .driverId(model.getDriver() == null ? null : model.getDriver().getId())
                .documentType(model.getDocumentType())
                .documentUrl(model.getDocumentUrl())
                .verificationStatus(model.getVerificationStatus())
                .uploadedAt(model.getUploadedAt())
                .verifiedAt(model.getVerifiedAt())
                .remarks(model.getRemarks())
                .build();
    }
}
