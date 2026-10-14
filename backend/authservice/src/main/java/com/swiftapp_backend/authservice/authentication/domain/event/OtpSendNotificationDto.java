package com.swiftapp_backend.authservice.authentication.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class OtpSendNotificationDto {
    private String subject;
    private String htmlContent;
    private String email;
}
