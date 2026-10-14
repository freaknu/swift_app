package com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.adapter;

import com.swiftapp_backend.matchingservice.matching.domain.dto.DriverFindDto;
import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.models.DriverLocation;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverMatchingRepository;
import com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.entity.DriverLocationEntity;
import com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.mapper.DriverLocationPersistenceMapper;
import com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.repository.DriverMatchingRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DriverMatchingAdapter
        implements DriverMatchingRepository {

    private final DriverMatchingRedisRepository
            driverMatchingRedisRepository;

    @Override
    public void updateDriverLocation(
            DriverLocation location
    ) {

        DriverLocationEntity entity =
                DriverLocationPersistenceMapper.toEntity(
                        location
                );

        driverMatchingRedisRepository
                .updateDriverLocation(entity);
    }

    @Override
    public List<DriverLocation> findAllNearbyDriver(
            DriverFindDto data
    ) {

        List<DriverLocationEntity> entities =
                driverMatchingRedisRepository
                        .findAllNearbyDriver(
                                data.getLatitude(),
                                data.getLongitude(),
                                data.getVehicleTypeEnum(),
                                data.getRadiusInKilometers(),
                                data.getLimit()
                        );

        return entities.stream()
                .map(
                        DriverLocationPersistenceMapper::toDomain
                )
                .toList();
    }

    @Override
    public void updateDriverStatus(Long driverId, DriverStatusEventEnum status) {
        driverMatchingRedisRepository.updateDriverStatus(driverId, status);
    }
}
