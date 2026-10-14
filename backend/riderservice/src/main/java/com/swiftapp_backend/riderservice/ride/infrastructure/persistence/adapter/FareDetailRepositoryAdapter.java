package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.adapter;

import com.swiftapp_backend.riderservice.ride.domain.model.FareDetailModel;
import com.swiftapp_backend.riderservice.ride.domain.repository.FareDetailRepository;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.FareDetailEntity;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.mapper.RidePersistenceMapper;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.JpaFareDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FareDetailRepositoryAdapter implements FareDetailRepository {
    private final JpaFareDetailRepository repository;
    private final RidePersistenceMapper mapper;

    @Override
    @Transactional
    public FareDetailModel save(FareDetailModel fareDetail) {
        FareDetailEntity saved = repository.save(mapper.toEntity(fareDetail));
        FareDetailModel model = mapper.toDomain(saved);
        model.setRide(fareDetail.getRide());
        return model;
    }

    @Override
    public Optional<FareDetailModel> findByRideId(Long rideId) {
        return repository.findByRide_Id(rideId).map(mapper::toDomain);
    }
}
