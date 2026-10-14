package com.swiftapp_backend.authservice.authentication.domain.model;

import com.swiftapp_backend.authservice.authentication.domain.enums.AppUserRoles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class AppUser {
    private Long id;
    private String name;
    private String email;
    private boolean accountLocked;
    private boolean isActive;
    private AppUserRoles role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
