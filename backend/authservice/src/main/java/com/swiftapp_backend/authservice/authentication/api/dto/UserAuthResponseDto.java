package com.swiftapp_backend.authservice.authentication.api.dto;

import com.swiftapp_backend.authservice.authentication.domain.enums.AppUserRoles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class UserAuthResponseDto {
    private Long userId;
    private String name;
    private String email;
    private boolean accountLocked;
    private boolean isActive;
    private boolean deviceLocked;
    // if this device was already locked then check this and match
    private int pinCode;
    private String accessToken;
    private String refreshToken;
    private AppUserRoles role;
}
