package com.swiftapp_backend.driverservice.driver.domain.repository;

import com.swiftapp_backend.driverservice.driver.domain.enums.DriverRideStatisticsEventType;

public interface DriverRideStatisticsEventRepository {
    boolean lockDriver(Long driverId);

    boolean hasRecordedRide(Long rideId);

    void recordRide(Long rideId, Long driverId, DriverRideStatisticsEventType eventType);
}
