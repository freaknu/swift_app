package com.swiftapp_backend.userservice.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class EmergencyContactModel {
    private Long id;

    private String contactType;
    private String contactPersonName;
    private String contactPersonNumber;

    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserModel user;
}
