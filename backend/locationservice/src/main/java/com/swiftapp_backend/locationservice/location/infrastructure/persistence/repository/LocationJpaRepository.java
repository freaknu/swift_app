package com.swiftapp_backend.locationservice.location.infrastructure.persistence.repository;

import com.swiftapp_backend.locationservice.location.infrastructure.persistence.entity.LocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationJpaRepository extends JpaRepository<LocationEntity,Long> {
    List<LocationEntity> findAllByRideIdAndDriverIdOrderByCreatedAtAsc(Long rideId, Long driverId);
    List<LocationEntity> findAllByRideIdAndRiderIdOrderByCreatedAtAsc(Long rideId, Long riderId);
    List<LocationEntity> findAllByRideIdAndDriverId(Long rideId,Long driverId);
    Optional<LocationEntity> findFirstByRideIdAndDriverIdOrderByCreatedAtDescIdDesc(Long rideId, Long driverId);
}
