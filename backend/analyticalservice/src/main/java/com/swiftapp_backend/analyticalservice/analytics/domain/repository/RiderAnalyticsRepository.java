package com.swiftapp_backend.analyticalservice.analytics.domain.repository;

import com.swiftapp_backend.analyticalservice.analytics.domain.model.RiderAnalytics;

import java.util.Optional;

public interface RiderAnalyticsRepository {
    RiderAnalytics save(RiderAnalytics analytics);

    Optional<RiderAnalytics> findByRiderId(Long riderId);

    long count();

    double sumTotalSpent();
}
