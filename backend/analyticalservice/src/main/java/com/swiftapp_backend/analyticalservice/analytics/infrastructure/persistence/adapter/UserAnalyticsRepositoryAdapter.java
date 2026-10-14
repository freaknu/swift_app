package com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.adapter;

import com.swiftapp_backend.analyticalservice.analytics.domain.model.UserAnalytics;
import com.swiftapp_backend.analyticalservice.analytics.domain.repository.UserAnalyticsRepository;
import com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.mapper.AnalyticsPersistenceMapper;
import com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.repository.UserAnalyticsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserAnalyticsRepositoryAdapter implements UserAnalyticsRepository {
    private final UserAnalyticsJpaRepository repository;

    @Override
    public UserAnalytics save(UserAnalytics analytics) {
        return AnalyticsPersistenceMapper.toDomain(repository.save(AnalyticsPersistenceMapper.toEntity(analytics)));
    }

    @Override
    public Optional<UserAnalytics> findByAuthUserId(Long authUserId) {
        return repository.findByAuthUserId(authUserId).map(AnalyticsPersistenceMapper::toDomain);
    }
}
