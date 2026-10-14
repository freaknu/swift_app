package com.swiftapp_backend.analyticalservice.analytics.domain.repository;

import com.swiftapp_backend.analyticalservice.analytics.domain.model.UserAnalytics;

import java.util.Optional;

public interface UserAnalyticsRepository {
    UserAnalytics save(UserAnalytics analytics);

    Optional<UserAnalytics> findByAuthUserId(Long authUserId);
}
