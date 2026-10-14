package com.swiftapp_backend.driverservice.driver.api.dto;

import com.swiftapp_backend.driverservice.driver.domain.enums.DocumentType;
import com.swiftapp_backend.driverservice.driver.domain.enums.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDocumentResponseDto {

    private Long id;

    private Long driverId;

    private DocumentType documentType;

    private String documentUrl;

    private VerificationStatus verificationStatus;

    private LocalDateTime uploadedAt;

    private LocalDateTime verifiedAt;

    private String remarks;
}