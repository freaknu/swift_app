package com.swiftapp_backend.locationservice.location.domain.repository;

import com.swiftapp_backend.locationservice.location.domain.model.Location;

import java.util.List;
import java.util.Optional;

public interface LocationRepository {
    Location save(Location location);

    List<Location> findAllByRideIdAndDriverIdOrderByCreatedAtAsc(Long rideId, Long driverId);

    List<Location> findAllByRideIdAndRiderIdOrderByCreatedAtAsc(Long rideId, Long riderId);

    List<Location> findAllByRideIdAndDriverId(Long rideId, Long driverId);
    Optional<Location> findLatestByRideIdAndDriverId(Long rideId, Long driverId);
}
