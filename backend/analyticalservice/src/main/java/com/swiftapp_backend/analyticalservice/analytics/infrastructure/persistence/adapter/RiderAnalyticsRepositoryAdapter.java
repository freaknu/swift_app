package com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.adapter;

import com.swiftapp_backend.analyticalservice.analytics.domain.model.RiderAnalytics;
import com.swiftapp_backend.analyticalservice.analytics.domain.repository.RiderAnalyticsRepository;
import com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.mapper.AnalyticsPersistenceMapper;
import com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.repository.RiderAnalyticsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RiderAnalyticsRepositoryAdapter implements RiderAnalyticsRepository {
    private final RiderAnalyticsJpaRepository repository;

    @Override
    public RiderAnalytics save(RiderAnalytics analytics) {
        return AnalyticsPersistenceMapper.toDomain(repository.save(AnalyticsPersistenceMapper.toEntity(analytics)));
    }

    @Override
    public Optional<RiderAnalytics> findByRiderId(Long riderId) {
        return repository.findByRiderId(riderId).map(AnalyticsPersistenceMapper::toDomain);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public double sumTotalSpent() {
        return repository.findAll().stream().mapToDouble(entity -> entity.getTotalSpent() == null ? 0.0 : entity.getTotalSpent()).sum();
    }
}
