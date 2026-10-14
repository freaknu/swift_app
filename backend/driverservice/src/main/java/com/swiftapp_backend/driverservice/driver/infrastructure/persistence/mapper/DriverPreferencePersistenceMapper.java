package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.mapper;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverPreferenceModel;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverPreferenceEntity;

import java.util.LinkedHashSet;

public final class DriverPreferencePersistenceMapper {
    private DriverPreferencePersistenceMapper() {
    }

    public static DriverPreferenceModel toDomain(DriverPreferenceEntity source) {
        if (source == null) {
            return null;
        }

        DriverPreferenceModel target = new DriverPreferenceModel();
        target.setId(source.getId());
        target.setDriver(DriverPersistenceMapper.toDomainReference(source.getDriver()));
        target.setAutoAccept(source.isAutoAccept());
        target.setAcceptCash(source.isAcceptCash());
        target.setAcceptOnline(source.isAcceptOnline());
        target.setPreferredRadius(source.getPreferredRadius());
        target.setPreferredRideTypes(source.getPreferredRideTypes() == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(source.getPreferredRideTypes()));
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    public static DriverPreferenceEntity toEntity(
            DriverPreferenceModel source,
            DriverEntity driver
    ) {
        if (source == null) {
            return null;
        }

        DriverPreferenceEntity target = new DriverPreferenceEntity();
        target.setId(source.getId());
        updateEntity(source, target, driver);
        return target;
    }

    public static void updateEntity(
            DriverPreferenceModel source,
            DriverPreferenceEntity target,
            DriverEntity driver
    ) {
        target.setDriver(driver);
        target.setAutoAccept(source.isAutoAccept());
        target.setAcceptCash(source.isAcceptCash());
        target.setAcceptOnline(source.isAcceptOnline());
        target.setPreferredRadius(source.getPreferredRadius());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        if (source.getPreferredRideTypes() != null) {
            if (target.getPreferredRideTypes() == null) {
                target.setPreferredRideTypes(new LinkedHashSet<>(source.getPreferredRideTypes()));
            } else {
                target.getPreferredRideTypes().clear();
                target.getPreferredRideTypes().addAll(source.getPreferredRideTypes());
            }
        }
    }
}
