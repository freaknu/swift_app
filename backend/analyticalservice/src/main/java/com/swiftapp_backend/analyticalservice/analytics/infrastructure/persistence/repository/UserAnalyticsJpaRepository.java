package com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.repository;

import com.swiftapp_backend.analyticalservice.analytics.infrastructure.persistence.entity.UserAnalyticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserAnalyticsJpaRepository extends JpaRepository<UserAnalyticsEntity,Long> {
    Optional<UserAnalyticsEntity> findByAuthUserId(Long authUserId);
}
