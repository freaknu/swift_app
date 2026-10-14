package com.swiftapp_backend.paymentservice.payment.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class AccountDetails {
    private Long userId;
    private String bankName;
    private String accountHolderName;
    private String bankAccountNumber;
    private String ifscCode;
    private String registeredPhoneNumber;
    private boolean isVerified;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
