package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository;

import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEarningSummaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataDriverEarningRepository extends JpaRepository<DriverEarningSummaryEntity,Long> {
    Optional<DriverEarningSummaryEntity> findByDriver_Id(Long driverId);
}
