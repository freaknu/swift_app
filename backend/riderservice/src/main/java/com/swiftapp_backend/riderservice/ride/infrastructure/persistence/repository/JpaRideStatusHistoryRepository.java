package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository;

import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideStatusHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaRideStatusHistoryRepository extends JpaRepository<RideStatusHistoryEntity,Long> {
    List<RideStatusHistoryEntity> findAllByRide_IdOrderByCreatedAtAsc(Long rideId);
}
