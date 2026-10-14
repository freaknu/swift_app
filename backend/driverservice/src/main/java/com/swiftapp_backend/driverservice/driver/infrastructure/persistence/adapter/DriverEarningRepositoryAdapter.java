package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverEarningSummaryModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverEarningRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.mapper.DriverEarningSummaryPersistenceMapper;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverEarningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DriverEarningRepositoryAdapter implements DriverEarningRepository {

    private final SpringDataDriverEarningRepository repository;
    private final DriverEarningSummaryPersistenceMapper mapper;

    @Override
    public Optional<DriverEarningSummaryModel> findByDriverId(Long driverId) {
        return repository.findByDriver_Id(driverId).map(mapper::toDomain);
    }

    @Override
    public DriverEarningSummaryModel save(DriverEarningSummaryModel model) {
        return mapper.toDomain(repository.save(mapper.toEntity(model)));
    }
}
