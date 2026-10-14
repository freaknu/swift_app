package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.adapter;

import com.swiftapp_backend.riderservice.ride.domain.model.RideUserModel;
import com.swiftapp_backend.riderservice.ride.domain.repository.RideUserRepository;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideUserEntity;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.mapper.RidePersistenceMapper;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.JpaRideUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RideUserRepositoryAdapter implements RideUserRepository {
    private final JpaRideUserRepository repository;
    private final RidePersistenceMapper mapper;

    @Override
    @Transactional
    public RideUserModel save(RideUserModel rideUser) {
        RideUserEntity saved = repository.save(mapper.toEntity(rideUser));
        RideUserModel model = mapper.toDomain(saved);
        model.setRide(rideUser.getRide());
        return model;
    }

    @Override
    public Optional<RideUserModel> findByRideId(Long rideId) {
        return repository.findByRide_Id(rideId).map(mapper::toDomain);
    }
}
