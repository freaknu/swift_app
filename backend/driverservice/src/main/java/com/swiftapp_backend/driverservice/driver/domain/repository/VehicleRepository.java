package com.swiftapp_backend.driverservice.driver.domain.repository;

import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository {

    Optional<VehicleModel> findById(Long id);

    List<VehicleModel> findAllByDriverId(Long driverId);

    List<VehicleModel> findAllByAuthUserId(Long authUserId);

    Optional<VehicleModel> findByAuthUserIdAndId(Long authUserId, Long vehicleId);

    VehicleModel save(VehicleModel vehicle);

    List<VehicleModel> saveAll(Iterable<VehicleModel> vehicles);
}
