package com.swiftapp_backend.riderservice.ride.domain.repository;

import com.swiftapp_backend.riderservice.ride.domain.model.RideUserModel;

import java.util.Optional;

public interface RideUserRepository {
    RideUserModel save(RideUserModel rideUser);

    Optional<RideUserModel> findByRideId(Long rideId);
}
