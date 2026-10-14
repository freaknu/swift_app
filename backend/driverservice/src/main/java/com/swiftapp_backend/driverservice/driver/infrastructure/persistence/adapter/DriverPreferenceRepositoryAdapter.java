package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverPreferenceModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverPreferenceRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverPreferenceEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.mapper.DriverPreferencePersistenceMapper;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverPreferenceRepositoryAdapter implements DriverPreferenceRepository {

    private final SpringDataDriverPreferenceRepository jpaRepository;
    private final SpringDataDriverRepository driverJpaRepository;

    @Override
    public Optional<DriverPreferenceModel> findById(Long id) {
        return jpaRepository.findById(id).map(DriverPreferencePersistenceMapper::toDomain);
    }

    @Override
    public Optional<DriverPreferenceModel> findByDriverId(Long driverId) {
        return jpaRepository.findByDriver_Id(driverId)
                .map(DriverPreferencePersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public DriverPreferenceModel save(DriverPreferenceModel preference) {
        DriverEntity driver = resolveDriver(preference);
        DriverPreferenceEntity entity = preference.getId() == null
                ? DriverPreferencePersistenceMapper.toEntity(preference, driver)
                : jpaRepository.findById(preference.getId())
                        .map(existing -> {
                            DriverPreferencePersistenceMapper.updateEntity(
                                    preference,
                                    existing,
                                    driver
                            );
                            return existing;
                        })
                        .orElseGet(() -> DriverPreferencePersistenceMapper.toEntity(
                                preference,
                                driver
                        ));
        return DriverPreferencePersistenceMapper.toDomain(jpaRepository.save(entity));
    }

    private DriverEntity resolveDriver(DriverPreferenceModel preference) {
        if (preference.getDriver() == null) {
            return null;
        }
        if (preference.getDriver().getId() == null) {
            throw new IllegalArgumentException("Driver preferences must reference a persisted driver");
        }
        return driverJpaRepository.getReferenceById(preference.getDriver().getId());
    }
}
