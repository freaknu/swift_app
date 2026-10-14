package com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.adapter;

import com.swiftapp_backend.analyticalservice.analytics.domain.model.DriverAnalytics;
import com.swiftapp_backend.analyticalservice.analytics.domain.repository.DriverAnalyticsRepository;
import com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.mapper.AnalyticsPersistenceMapper;
import com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.repository.DriverAnalyticsJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DriverAnalyticsRepositoryAdapter implements DriverAnalyticsRepository {
    private final DriverAnalyticsJpaRepository repository;

    @Override
    public DriverAnalytics save(DriverAnalytics analytics) {
        return AnalyticsPersistenceMapper.toDomain(repository.save(AnalyticsPersistenceMapper.toEntity(analytics)));
    }

    @Override
    public Optional<DriverAnalytics> findByDriverId(Long driverId) {
        return repository.findByDriverId(driverId).map(AnalyticsPersistenceMapper::toDomain);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public long sumCompletedTrips() {
        return repository.findAll().stream().mapToLong(entity -> value(entity.getCompletedTrips())).sum();
    }

    @Override
    public long sumCancelledTrips() {
        return repository.findAll().stream().mapToLong(entity -> value(entity.getCancelledTrips())).sum();
    }

    @Override
    public double sumTotalEarnings() {
        return repository.findAll().stream().mapToDouble(entity -> entity.getTotalEarnings() == null ? 0.0 : entity.getTotalEarnings()).sum();
    }

    private static long value(Long value) {
        return value == null ? 0L : value;
    }
}
