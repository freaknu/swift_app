package com.swiftapp_backend.driverservice.driver.application.port;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverNotificationSettings;

import java.util.Optional;

public interface DriverNotificationSettingsPort {
    Optional<DriverNotificationSettings> find(Long driverId, Long authUserId);
    DriverNotificationSettings save(DriverNotificationSettings settings);
}
