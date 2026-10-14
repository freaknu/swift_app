package com.swiftapp_backend.riderservice.ride.domain.repository;

import com.swiftapp_backend.riderservice.ride.domain.model.RideStatusHistoryModel;

import java.util.List;

public interface RideStatusHistoryRepository {
    RideStatusHistoryModel save(RideStatusHistoryModel history);

    List<RideStatusHistoryModel> findAllByRideIdOrderByCreatedAtAsc(Long rideId);
}
