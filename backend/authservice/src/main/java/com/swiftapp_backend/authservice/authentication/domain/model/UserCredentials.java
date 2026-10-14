package com.swiftapp_backend.authservice.authentication.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class UserCredentials {
    private Long id;
    private Long userId;
    private String accessToken;
    private String refreshToken;
    private String deviceName;
    private String os;
    private String deviceModel;
    private String fingerPrint;
    private String ipAddress;
    private int appLockPin;
    private boolean isAppLocked;
    private boolean isLoggedOut;
    private LocalDateTime loggedInAt;
}
