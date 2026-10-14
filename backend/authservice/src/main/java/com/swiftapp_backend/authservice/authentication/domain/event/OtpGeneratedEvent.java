package com.swiftapp_backend.authservice.authentication.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class OtpGeneratedEvent {
    private String email;
    private int otp;
    private LocalDateTime generatedAt;
    private long expiryMinutes;
}
