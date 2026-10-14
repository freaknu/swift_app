package com.swiftapp_backend.driverservice.driver.domain.repository;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverPreferenceModel;

import java.util.Optional;

public interface DriverPreferenceRepository {

    Optional<DriverPreferenceModel> findById(Long id);

    Optional<DriverPreferenceModel> findByDriverId(Long driverId);

    DriverPreferenceModel save(DriverPreferenceModel preference);
}
