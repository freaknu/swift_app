package com.swiftapp_backend.driverservice.driver.domain.repository;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;

import java.util.Optional;

public interface DriverRepository {

    Optional<DriverModel> findById(Long id);

    Optional<DriverModel> findByAuthUserId(Long authUserId);

    Optional<DriverModel> findByEmail(String email);

    DriverModel save(DriverModel driver);
}
