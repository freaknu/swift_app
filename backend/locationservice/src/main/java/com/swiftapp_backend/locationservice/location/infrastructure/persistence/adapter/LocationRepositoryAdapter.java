package com.swiftapp_backend.locationservice.location.infrastructure.persistence.adapter;

import com.swiftapp_backend.locationservice.location.domain.model.Location;
import com.swiftapp_backend.locationservice.location.domain.repository.LocationRepository;
import com.swiftapp_backend.locationservice.location.infrastructure.persistence.mapper.LocationPersistenceMapper;
import com.swiftapp_backend.locationservice.location.infrastructure.persistence.repository.LocationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LocationRepositoryAdapter implements LocationRepository {
    private final LocationJpaRepository repository;

    @Override
    public Location save(Location location) {
        return LocationPersistenceMapper.toDomain(repository.save(LocationPersistenceMapper.toEntity(location)));
    }

    @Override
    public List<Location> findAllByRideIdAndDriverIdOrderByCreatedAtAsc(Long rideId, Long driverId) {
        return repository.findAllByRideIdAndDriverIdOrderByCreatedAtAsc(rideId, driverId).stream()
                .map(LocationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Location> findAllByRideIdAndRiderIdOrderByCreatedAtAsc(Long rideId, Long riderId) {
        return repository.findAllByRideIdAndRiderIdOrderByCreatedAtAsc(rideId, riderId).stream()
                .map(LocationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<Location> findAllByRideIdAndDriverId(Long rideId, Long driverId) {
        return repository.findAllByRideIdAndDriverId(rideId, driverId).stream()
                .map(LocationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Location> findLatestByRideIdAndDriverId(Long rideId, Long driverId) {
        return repository.findFirstByRideIdAndDriverIdOrderByCreatedAtDescIdDesc(rideId, driverId)
                .map(LocationPersistenceMapper::toDomain);
    }
}
