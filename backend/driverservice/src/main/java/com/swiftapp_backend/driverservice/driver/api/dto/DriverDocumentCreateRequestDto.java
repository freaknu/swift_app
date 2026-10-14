package com.swiftapp_backend.driverservice.driver.api.dto;

import com.swiftapp_backend.driverservice.driver.domain.enums.DocumentType;
import com.swiftapp_backend.driverservice.driver.domain.enums.VerificationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverDocumentCreateRequestDto {

        @NotNull(message = "Document type is required")
        private DocumentType documentType;

        @NotBlank(message = "Document URL is required")
        private String documentUrl;
}