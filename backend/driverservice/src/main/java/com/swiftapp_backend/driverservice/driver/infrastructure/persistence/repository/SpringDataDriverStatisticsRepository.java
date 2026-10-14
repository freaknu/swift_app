package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository;

import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverStatisticsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataDriverStatisticsRepository extends JpaRepository<DriverStatisticsEntity,Long> {
    Optional<DriverStatisticsEntity> findByDriver_Id(Long driverId);
}
