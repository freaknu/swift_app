package com.swiftapp_backend.riderservice.ride.domain.repository;

import com.swiftapp_backend.riderservice.ride.domain.model.FareDetailModel;

import java.util.Optional;

public interface FareDetailRepository {
    FareDetailModel save(FareDetailModel fareDetail);

    Optional<FareDetailModel> findByRideId(Long rideId);
}
