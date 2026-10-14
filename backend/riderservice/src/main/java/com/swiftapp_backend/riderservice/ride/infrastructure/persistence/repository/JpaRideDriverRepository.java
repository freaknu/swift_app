package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository;

import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideDriverEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaRideDriverRepository extends JpaRepository<RideDriverEntity,Long> {
     Optional<RideDriverEntity> findByRide_Id(Long rideId);
}
