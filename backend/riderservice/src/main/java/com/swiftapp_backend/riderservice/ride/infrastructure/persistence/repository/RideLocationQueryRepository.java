package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository;

import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RecentLocationEntity;

import java.util.List;

public interface RideLocationQueryRepository {

    List<RecentLocationEntity> findPreviousDestinations(
            Long riderId,
            double latitude,
            double longitude,
            double radiusMeters
    );
}
