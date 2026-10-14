package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository;

import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverMedicalInformationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpringDataDriverMedicalInformationRepository extends JpaRepository<DriverMedicalInformationEntity,Long> {
    Optional<DriverMedicalInformationEntity> findByAuthUserId(Long authUserId);
    Optional<DriverMedicalInformationEntity> findByDriver_Id(Long driverId);
}
