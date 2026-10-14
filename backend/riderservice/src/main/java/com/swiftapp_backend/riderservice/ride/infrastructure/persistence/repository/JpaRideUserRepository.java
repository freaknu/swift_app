package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository;

import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaRideUserRepository extends JpaRepository<RideUserEntity,Long> {
    Optional<RideUserEntity> findByRide_Id(Long rideId);
}
