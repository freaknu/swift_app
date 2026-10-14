package com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.repository;

import com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.entity.DriverAnalyticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverAnalyticsJpaRepository extends JpaRepository<DriverAnalyticsEntity,Long> {
	java.util.Optional<DriverAnalyticsEntity> findByDriverId(Long driverId);
}
