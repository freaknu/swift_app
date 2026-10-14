package com.swiftapp_backend.driverservice.driver.domain.model;

/** Durable settings owned by one authenticated driver's profile. */
public record DriverNotificationSettings(
        Long driverId, Long authUserId,
        boolean pushEnabled, boolean smsEnabled, boolean emailEnabled,
        boolean rideAlertsEnabled, boolean surgeAlertsEnabled) {}
