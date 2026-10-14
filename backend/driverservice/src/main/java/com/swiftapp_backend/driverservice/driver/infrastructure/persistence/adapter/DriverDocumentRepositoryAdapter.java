package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverDocumentModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.DriverDocumentRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverDocumentEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.mapper.DriverDocumentPersistenceMapper;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverDocumentRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverDocumentRepositoryAdapter implements DriverDocumentRepository {

    private final SpringDataDriverDocumentRepository jpaRepository;
    private final SpringDataDriverRepository driverJpaRepository;

    @Override
    public Optional<DriverDocumentModel> findById(Long id) {
        return jpaRepository.findById(id).map(DriverDocumentPersistenceMapper::toDomain);
    }

    @Override
    public List<DriverDocumentModel> findAllByDriverId(Long driverId) {
        return jpaRepository.findAllByDriver_Id(driverId).stream()
                .map(DriverDocumentPersistenceMapper::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<DriverDocumentModel> findAllByAuthUserId(Long authUserId) {
        return jpaRepository.findAllByAuthUserId(authUserId).stream()
                .map(DriverDocumentPersistenceMapper::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Optional<DriverDocumentModel> findByAuthUserIdAndId(Long authUserId, Long id) {
        return jpaRepository.findByAuthUserIdAndId(authUserId, id)
                .map(DriverDocumentPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public DriverDocumentModel save(DriverDocumentModel document) {
        DriverDocumentEntity entity = prepareEntity(document);
        return DriverDocumentPersistenceMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional
    public List<DriverDocumentModel> saveAll(Iterable<DriverDocumentModel> documents) {
        List<DriverDocumentEntity> entities = new ArrayList<>();
        documents.forEach(document -> entities.add(prepareEntity(document)));
        return jpaRepository.saveAll(entities).stream()
                .map(DriverDocumentPersistenceMapper::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private DriverDocumentEntity prepareEntity(DriverDocumentModel document) {
        DriverEntity driver = resolveDriver(document);
        if (document.getId() == null) {
            return DriverDocumentPersistenceMapper.toEntity(document, driver);
        }

        return jpaRepository.findById(document.getId())
                .map(existing -> {
                    DriverDocumentPersistenceMapper.updateEntity(document, existing, driver);
                    return existing;
                })
                .orElseGet(() -> DriverDocumentPersistenceMapper.toEntity(document, driver));
    }

    private DriverEntity resolveDriver(DriverDocumentModel document) {
        if (document.getDriver() == null) {
            return null;
        }
        if (document.getDriver().getId() == null) {
            throw new IllegalArgumentException("A document must reference a persisted driver");
        }
        return driverJpaRepository.getReferenceById(document.getDriver().getId());
    }
}
