package com.swiftapp_backend.riderservice.ride.infrastructure.persistence.adapter;

import com.swiftapp_backend.riderservice.ride.domain.model.OfferModel;
import com.swiftapp_backend.riderservice.ride.domain.repository.OfferRepository;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.mapper.RidePersistenceMapper;
import com.swiftapp_backend.riderservice.ride.infrastructure.persistence.repository.JpaOfferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OfferRepositoryAdapter implements OfferRepository {
    private final JpaOfferRepository repository;
    private final RidePersistenceMapper mapper;

    @Override
    public List<OfferModel> findActiveByRideTypeCode(String rideTypeCode) {
        return repository.findByRideTypeCodeAndActiveTrue(rideTypeCode)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
