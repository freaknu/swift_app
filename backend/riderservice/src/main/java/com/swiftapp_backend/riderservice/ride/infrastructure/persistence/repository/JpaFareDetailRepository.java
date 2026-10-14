package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository;

import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.FareDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaFareDetailRepository extends JpaRepository<FareDetailEntity,Long> {
    Optional<FareDetailEntity> findByRide_Id(Long rideId);
    boolean existsByRide_Id(Long rideId);
    void deleteByRide_Id(Long rideId);
}
