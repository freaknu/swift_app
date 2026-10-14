package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository;

import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataDriverPreferenceRepository extends JpaRepository<DriverPreferenceEntity,Long> {
    Optional<DriverPreferenceEntity> findByDriver_Id(Long driverId);
}
