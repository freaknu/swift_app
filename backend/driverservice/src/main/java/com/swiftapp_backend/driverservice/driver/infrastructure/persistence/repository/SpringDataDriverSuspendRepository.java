package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository;

import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverSuspendEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataDriverSuspendRepository extends JpaRepository<DriverSuspendEntity,Long> {
    Optional<DriverSuspendEntity> findByDriver_Id(Long driverId);
    List<DriverSuspendEntity> findAllByIsSuspended(Boolean isSuspended, Pageable pageable);
    List<DriverSuspendEntity> findAllBySuspendedById(Long suspendedById,Pageable pageable);
    List<DriverSuspendEntity> findAllByUnsuspendedById(Long unsuspendedById,Pageable pageable);
}
