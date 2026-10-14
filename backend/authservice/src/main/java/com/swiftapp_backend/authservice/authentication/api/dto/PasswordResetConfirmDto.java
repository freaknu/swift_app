package com.swiftapp_backend.authservice.authentication.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordResetConfirmDto {
    private String email;
    private int otp;
    private String newPassword;
}
