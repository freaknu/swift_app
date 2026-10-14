package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.adapter;

import com.swiftapp_backend.riderservice.ride.domain.model.RideStatusHistoryModel;
import com.swiftapp_backend.riderservice.ride.domain.repository.RideStatusHistoryRepository;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.entity.RideStatusHistoryEntity;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.mapper.RidePersistenceMapper;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.JpaRideStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RideStatusHistoryRepositoryAdapter implements RideStatusHistoryRepository {
    private final JpaRideStatusHistoryRepository repository;
    private final RidePersistenceMapper mapper;

    @Override
    @Transactional
    public RideStatusHistoryModel save(RideStatusHistoryModel history) {
        RideStatusHistoryEntity saved = repository.save(mapper.toEntity(history));
        RideStatusHistoryModel model = mapper.toDomain(saved);
        model.setRide(history.getRide());
        return model;
    }

    @Override
    public List<RideStatusHistoryModel> findAllByRideIdOrderByCreatedAtAsc(Long rideId) {
        return mapper.toHistoryDomain(repository.findAllByRide_IdOrderByCreatedAtAsc(rideId));
    }
}
