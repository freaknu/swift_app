package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverStatisticsModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverStatisticsRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverStatisticsEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.mapper.DriverStatisticsPersistenceMapper;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverStatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverStatisticsRepositoryAdapter implements DriverStatisticsRepository {

    private final SpringDataDriverStatisticsRepository jpaRepository;
    private final SpringDataDriverRepository driverJpaRepository;

    @Override
    public Optional<DriverStatisticsModel> findById(Long id) {
        return jpaRepository.findById(id).map(DriverStatisticsPersistenceMapper::toDomain);
    }

    @Override
    public Optional<DriverStatisticsModel> findByDriverId(Long driverId) {
        return jpaRepository.findByDriver_Id(driverId)
                .map(DriverStatisticsPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public DriverStatisticsModel save(DriverStatisticsModel statistics) {
        DriverEntity driver = resolveDriver(statistics);
        DriverStatisticsEntity entity = statistics.getId() == null
                ? DriverStatisticsPersistenceMapper.toEntity(statistics, driver)
                : jpaRepository.findById(statistics.getId())
                        .map(existing -> {
                            DriverStatisticsPersistenceMapper.updateEntity(
                                    statistics,
                                    existing,
                                    driver
                            );
                            return existing;
                        })
                        .orElseGet(() -> DriverStatisticsPersistenceMapper.toEntity(
                                statistics,
                                driver
                        ));
        return DriverStatisticsPersistenceMapper.toDomain(jpaRepository.save(entity));
    }

    private DriverEntity resolveDriver(DriverStatisticsModel statistics) {
        if (statistics.getDriver() == null) {
            return null;
        }
        if (statistics.getDriver().getId() == null) {
            throw new IllegalArgumentException("Driver statistics must reference a persisted driver");
        }
        return driverJpaRepository.getReferenceById(statistics.getDriver().getId());
    }
}
