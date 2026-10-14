package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.adapter;

import com.swiftapp_backend.riderservice.ride.domain.model.RideTypesModel;
import com.swiftapp_backend.riderservice.ride.domain.repository.RideTypesRepository;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.mapper.RidePersistenceMapper;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.JpaRideTypesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RideTypesRepositoryAdapter implements RideTypesRepository {
    private final JpaRideTypesRepository repository;
    private final RidePersistenceMapper mapper;

    @Override
    public List<RideTypesModel> findAllBySortOrderAsc() {
        return repository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder"))
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<RideTypesModel> findActiveOrderBySortOrderAsc() {
        return repository.findByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
