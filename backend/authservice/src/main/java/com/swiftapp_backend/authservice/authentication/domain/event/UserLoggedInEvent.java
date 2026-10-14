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
public class UserLoggedInEvent {
    private Long authUserId;
    private String email;
    private String deviceName;
    private String ipAddress;
    private LocalDateTime loggedInAt;
}
