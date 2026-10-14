package com.swiftapp_backend.analyticalservice.analytics.domain.model;

import com.swiftapp_backend.analyticalservice.analytics.domain.enums.AppUserRoles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAnalytics {
    private Long id;
    private Long authUserId;
    private AppUserRoles role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
