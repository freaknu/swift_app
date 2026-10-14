package com.swiftapp_backend.driverservice.driver.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverNotificationPreferencesDto {
    private boolean pushEnabled;
    private boolean smsEnabled;
    private boolean emailEnabled;
    private boolean rideAlertsEnabled;
    private boolean surgeAlertsEnabled;
}
