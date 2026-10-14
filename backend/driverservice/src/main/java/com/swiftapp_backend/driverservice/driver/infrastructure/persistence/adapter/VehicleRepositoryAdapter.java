package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.adapter;

import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;
import com.swiftapp_backend.driverservice.driver.domain.repository.VehicleRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.VehicleEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.mapper.VehiclePersistenceMapper;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataDriverRepository;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.repository.SpringDataVehicleRepository;
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
public class VehicleRepositoryAdapter implements VehicleRepository {

    private final SpringDataVehicleRepository jpaRepository;
    private final SpringDataDriverRepository driverJpaRepository;

    @Override
    public Optional<VehicleModel> findById(Long id) {
        return jpaRepository.findById(id).map(VehiclePersistenceMapper::toDomain);
    }

    @Override
    public List<VehicleModel> findAllByDriverId(Long driverId) {
        return jpaRepository.findAllByDriver_Id(driverId).stream()
                .map(VehiclePersistenceMapper::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public List<VehicleModel> findAllByAuthUserId(Long authUserId) {
        return jpaRepository.findAllByAuthUserId(authUserId).stream()
                .map(VehiclePersistenceMapper::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Optional<VehicleModel> findByAuthUserIdAndId(Long authUserId, Long vehicleId) {
        return jpaRepository.findByAuthUserIdAndId(authUserId, vehicleId)
                .map(VehiclePersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public VehicleModel save(VehicleModel vehicle) {
        VehicleEntity entity = prepareEntity(vehicle);
        return VehiclePersistenceMapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional
    public List<VehicleModel> saveAll(Iterable<VehicleModel> vehicles) {
        List<VehicleEntity> entities = new ArrayList<>();
        vehicles.forEach(vehicle -> entities.add(prepareEntity(vehicle)));
        return jpaRepository.saveAll(entities).stream()
                .map(VehiclePersistenceMapper::toDomain)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private VehicleEntity prepareEntity(VehicleModel vehicle) {
        DriverEntity driver = resolveDriver(vehicle);
        if (vehicle.getId() == null) {
            return VehiclePersistenceMapper.toEntity(vehicle, driver);
        }

        return jpaRepository.findById(vehicle.getId())
                .map(existing -> {
                    VehiclePersistenceMapper.updateEntity(vehicle, existing, driver);
                    return existing;
                })
                .orElseGet(() -> VehiclePersistenceMapper.toEntity(vehicle, driver));
    }

    private DriverEntity resolveDriver(VehicleModel vehicle) {
        if (vehicle.getDriver() == null) {
            return null;
        }
        if (vehicle.getDriver().getId() == null) {
            throw new IllegalArgumentException("A vehicle must reference a persisted driver");
        }
        return driverJpaRepository.getReferenceById(vehicle.getDriver().getId());
    }
}
