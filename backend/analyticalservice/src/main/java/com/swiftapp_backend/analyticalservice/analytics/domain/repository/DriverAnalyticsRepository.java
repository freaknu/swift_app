package com.swiftapp_backend.analyticalservice.analytics.domain.repository;

import com.swiftapp_backend.analyticalservice.analytics.domain.model.DriverAnalytics;

import java.util.Optional;

public interface DriverAnalyticsRepository {
    DriverAnalytics save(DriverAnalytics analytics);

    Optional<DriverAnalytics> findByDriverId(Long driverId);

    long count();

    long sumCompletedTrips();

    long sumCancelledTrips();

    double sumTotalEarnings();
}
