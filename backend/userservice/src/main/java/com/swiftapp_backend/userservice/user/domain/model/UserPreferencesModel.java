package com.swiftapp_backend.userservice.user.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferencesModel {
    private Long id;
    private Long userId;
    private String language;
    private String theme; // LIGHT, DARK, SYSTEM
    private boolean pushNotificationEnabled;
    private boolean emailNotificationEnabled;
    private boolean smsNotificationEnabled;
    private String preferredPaymentMethod;
    private LocalDateTime updatedAt;
}
