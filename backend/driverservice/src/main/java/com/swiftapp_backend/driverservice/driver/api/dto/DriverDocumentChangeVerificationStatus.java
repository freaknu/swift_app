package com.swiftapp_backend.driverservice.driver.api.dto;

import com.swiftapp_backend.driverservice.driver.domain.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverDocumentChangeVerificationStatus {
    private Long driverId;
    private Long verifiedBy;
    private Long documentId;
    private VerificationStatus verificationStatus;
    private String remarks;
}
