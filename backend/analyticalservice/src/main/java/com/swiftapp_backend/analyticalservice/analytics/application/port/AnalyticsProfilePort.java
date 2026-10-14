package com.swiftapp_backend.analyticalservice.analytics.application.port;

public interface AnalyticsProfilePort {
    Long riderAuthUserId(Long riderId);
    Long driverAuthUserId(Long driverId);
}
