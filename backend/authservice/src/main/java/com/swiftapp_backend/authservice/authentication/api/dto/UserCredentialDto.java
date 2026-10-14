package com.swiftapp_backend.authservice.authentication.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class UserCredentialDto {
    private Long id;
    private Long userId;
    private String deviceName;
    private String os;
    private String deviceModel;
    private boolean isLoggedOut;
    private LocalDateTime loggedInAt;
}
