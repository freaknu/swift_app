package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository;

import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataVehicleRepository extends JpaRepository<VehicleEntity,Long> {
    List<VehicleEntity> findAllByDriver_Id(Long driverId);
    List<VehicleEntity> findAllByAuthUserId(Long authUserId);
    Optional<VehicleEntity> findByAuthUserIdAndId(Long authUserId, Long vehicleId);
}
