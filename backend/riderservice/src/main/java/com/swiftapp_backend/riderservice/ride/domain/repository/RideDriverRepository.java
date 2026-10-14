package com.swiftapp_backend.riderservice.ride.domain.repository;

import com.swiftapp_backend.riderservice.ride.domain.model.RideDriverModel;

import java.util.Optional;

public interface RideDriverRepository {
    RideDriverModel save(RideDriverModel rideDriver);

    Optional<RideDriverModel> findByRideId(Long rideId);

    void delete(RideDriverModel rideDriver);
}
