package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.mapper.DriverPersistenceMapper;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverRepositoryAdapter implements DriverRepository {

    private final SpringDataDriverRepository jpaRepository;

    @Override
    public Optional<DriverModel> findById(Long id) {
        return jpaRepository.findById(id).map(DriverPersistenceMapper::toDomain);
    }

    @Override
    public Optional<DriverModel> findByAuthUserId(Long authUserId) {
        return jpaRepository.findByAuthUserId(authUserId)
                .map(DriverPersistenceMapper::toDomain);
    }

    @Override
    public Optional<DriverModel> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(DriverPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public DriverModel save(DriverModel driver) {
        DriverEntity entity = driver.getId() == null
                ? DriverPersistenceMapper.toEntity(driver)
                : jpaRepository.findById(driver.getId())
                        .map(existing -> {
                            DriverPersistenceMapper.updateEntity(driver, existing);
                            return existing;
                        })
                        .orElseGet(() -> DriverPersistenceMapper.toEntity(driver));

        return DriverPersistenceMapper.toDomain(jpaRepository.save(entity));
    }
}
