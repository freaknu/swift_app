package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.adapter;

import com.swiftapp_backend.riderservice.ride.domain.model.RideDriverModel;
import com.swiftapp_backend.riderservice.ride.domain.repository.RideDriverRepository;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideDriverEntity;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.mapper.RidePersistenceMapper;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.JpaRideDriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RideDriverRepositoryAdapter implements RideDriverRepository {
    private final JpaRideDriverRepository repository;
    private final RidePersistenceMapper mapper;

    @Override
    @Transactional
    public RideDriverModel save(RideDriverModel rideDriver) {
        RideDriverEntity saved = repository.save(mapper.toEntity(rideDriver));
        RideDriverModel model = mapper.toDomain(saved);
        model.setRide(rideDriver.getRide());
        return model;
    }

    @Override
    public Optional<RideDriverModel> findByRideId(Long rideId) {
        return repository.findByRide_Id(rideId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void delete(RideDriverModel rideDriver) {
        repository.delete(mapper.toEntity(rideDriver));
    }
}
