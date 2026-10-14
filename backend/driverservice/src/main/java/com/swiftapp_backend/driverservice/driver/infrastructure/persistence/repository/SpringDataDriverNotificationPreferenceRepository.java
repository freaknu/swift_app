package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository;

import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverNotificationPreferencesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataDriverNotificationPreferenceRepository extends JpaRepository<DriverNotificationPreferencesEntity,Long> {
    Optional<DriverNotificationPreferencesEntity> findByDriver_Id(Long driverId);
    Optional<DriverNotificationPreferencesEntity> findByAuthUserId(Long authUserId);
}
