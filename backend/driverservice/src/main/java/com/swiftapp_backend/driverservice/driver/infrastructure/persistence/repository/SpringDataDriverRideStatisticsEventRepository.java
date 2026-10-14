package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository;

import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverRideStatisticsEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataDriverRideStatisticsEventRepository
        extends JpaRepository<DriverRideStatisticsEventEntity, Long> {
    boolean existsByRideId(Long rideId);
}
