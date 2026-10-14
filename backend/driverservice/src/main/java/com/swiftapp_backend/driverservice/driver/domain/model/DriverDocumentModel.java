package com.swiftapp_backend.driverservice.driver.domain.model;

import com.swiftapp_backend.driverservice.driver.domain.enums.DocumentType;
import com.swiftapp_backend.driverservice.driver.domain.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverDocumentModel {
    private Long id;

    private DriverModel driver;

    private DocumentType documentType;

    private Long authUserId;
    private boolean isActive;

    private String documentUrl;
    private VerificationStatus verificationStatus;

    private LocalDateTime uploadedAt;

    private LocalDateTime verifiedAt;

    private Long verifiedBy;

    private String remarks;

    private Set<VerificationStatus> allStatus;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
