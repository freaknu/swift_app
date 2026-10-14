package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository;

import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

@Repository
public interface SpringDataDriverRepository extends JpaRepository<DriverEntity,Long> {
    Optional<DriverEntity> findByAuthUserId(Long authUserId);
    Optional<DriverEntity> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select driver from DriverModel driver where driver.id = :id")
    Optional<DriverEntity> findByIdForNotificationUpdate(@Param("id") Long id);
}
