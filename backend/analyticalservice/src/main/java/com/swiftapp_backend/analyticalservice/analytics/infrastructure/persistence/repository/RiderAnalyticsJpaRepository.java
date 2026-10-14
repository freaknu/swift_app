package com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.repository;

import com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.entity.RiderAnalyticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RiderAnalyticsJpaRepository extends JpaRepository<RiderAnalyticsEntity,Long> {
	java.util.Optional<RiderAnalyticsEntity> findByRiderId(Long riderId);
}
