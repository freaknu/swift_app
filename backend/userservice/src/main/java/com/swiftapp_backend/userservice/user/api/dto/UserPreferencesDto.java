package com.swiftapp_backend.userservice.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPreferencesDto {
    private String language;
    private String theme;
    private boolean pushNotificationEnabled;
    private boolean emailNotificationEnabled;
    private boolean smsNotificationEnabled;
    private String preferredPaymentMethod;
}
