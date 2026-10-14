package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.mapper;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverDocumentModel;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverDocumentEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.VehicleEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class DriverPersistenceMapper {
    private DriverPersistenceMapper() {
    }

    public static DriverModel toDomain(DriverEntity source) {
        if (source == null) {
            return null;
        }

        DriverModel target = toDomainReference(source);
        target.setDriverPreferences(copyList(source.getDriverPreferences()));
        target.setLanguages(copyList(source.getLanguages()));

        if (source.getAllVehicles() != null) {
            List<VehicleModel> vehicles = source.getAllVehicles().stream()
                    .map(vehicle -> VehiclePersistenceMapper.toDomain(vehicle, target))
                    .collect(Collectors.toCollection(ArrayList::new));
            target.setAllVehicles(vehicles);
        }

        if (source.getAllDocuments() != null) {
            List<DriverDocumentModel> documents = source.getAllDocuments().stream()
                    .map(document -> DriverDocumentPersistenceMapper.toDomain(document, target))
                    .collect(Collectors.toCollection(ArrayList::new));
            target.setAllDocuments(documents);
        }

        return target;
    }

    static DriverModel toDomainReference(DriverEntity source) {
        if (source == null) {
            return null;
        }

        DriverModel target = new DriverModel();
        target.setId(source.getId());
        target.setAuthUserId(source.getAuthUserId());
        target.setDriverName(source.getDriverName());
        target.setEmail(source.getEmail());
        target.setPhoneNumber(source.getPhoneNumber());
        target.setGender(source.getGender());
        target.setProfileImage(source.getProfileImage());
        target.setActive(source.isActive());
        target.setDriverStatus(source.getDriverStatus());
        target.setAcceptingRides(source.isAcceptingRides());
        target.setDriverAvailability(source.getDriverAvailability());
        target.setVerified(source.isVerified());
        target.setDateOfBirth(source.getDateOfBirth());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    public static DriverEntity toEntity(DriverModel source) {
        if (source == null) {
            return null;
        }

        DriverEntity target = new DriverEntity();
        target.setId(source.getId());
        updateEntity(source, target);
        return target;
    }

    public static void updateEntity(DriverModel source, DriverEntity target) {
        target.setAuthUserId(source.getAuthUserId());
        target.setDriverName(source.getDriverName());
        target.setEmail(source.getEmail());
        target.setPhoneNumber(source.getPhoneNumber());
        target.setGender(source.getGender());
        target.setProfileImage(source.getProfileImage());
        target.setActive(source.isActive());
        target.setDriverStatus(source.getDriverStatus());
        target.setAcceptingRides(source.isAcceptingRides());
        target.setDriverAvailability(source.getDriverAvailability());
        target.setVerified(source.isVerified());
        target.setDateOfBirth(source.getDateOfBirth());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());

        if (source.getDriverPreferences() != null) {
            if (target.getDriverPreferences() == null) {
                target.setDriverPreferences(copyList(source.getDriverPreferences()));
            } else {
                target.getDriverPreferences().clear();
                target.getDriverPreferences().addAll(source.getDriverPreferences());
            }
        }
        if (source.getLanguages() != null) {
            if (target.getLanguages() == null) {
                target.setLanguages(copyList(source.getLanguages()));
            } else {
                target.getLanguages().clear();
                target.getLanguages().addAll(source.getLanguages());
            }
        }
        if (source.getAllVehicles() != null) {
            synchronizeVehicles(source.getAllVehicles(), target);
        }
        if (source.getAllDocuments() != null) {
            synchronizeDocuments(source.getAllDocuments(), target);
        }
    }

    private static void synchronizeVehicles(List<VehicleModel> source, DriverEntity driver) {
        List<VehicleEntity> current = driver.getAllVehicles();
        Map<Long, VehicleEntity> byId = new HashMap<>();
        if (current != null) {
            current.stream()
                    .filter(entity -> entity.getId() != null)
                    .forEach(entity -> byId.put(entity.getId(), entity));
        }

        List<VehicleEntity> synchronizedVehicles = new ArrayList<>();
        for (VehicleModel vehicle : source) {
            VehicleEntity entity = vehicle.getId() == null ? null : byId.get(vehicle.getId());
            if (entity == null) {
                entity = new VehicleEntity();
                entity.setId(vehicle.getId());
            }
            VehiclePersistenceMapper.updateEntity(vehicle, entity, driver);
            synchronizedVehicles.add(entity);
        }

        if (current == null) {
            driver.setAllVehicles(synchronizedVehicles);
        } else {
            current.clear();
            current.addAll(synchronizedVehicles);
        }
    }

    private static void synchronizeDocuments(List<DriverDocumentModel> source, DriverEntity driver) {
        List<DriverDocumentEntity> current = driver.getAllDocuments();
        Map<Long, DriverDocumentEntity> byId = new HashMap<>();
        if (current != null) {
            current.stream()
                    .filter(entity -> entity.getId() != null)
                    .forEach(entity -> byId.put(entity.getId(), entity));
        }

        List<DriverDocumentEntity> synchronizedDocuments = new ArrayList<>();
        for (DriverDocumentModel document : source) {
            DriverDocumentEntity entity = document.getId() == null ? null : byId.get(document.getId());
            if (entity == null) {
                entity = new DriverDocumentEntity();
                entity.setId(document.getId());
            }
            DriverDocumentPersistenceMapper.updateEntity(document, entity, driver);
            synchronizedDocuments.add(entity);
        }

        if (current == null) {
            driver.setAllDocuments(synchronizedDocuments);
        } else {
            current.clear();
            current.addAll(synchronizedDocuments);
        }
    }

    private static <T> List<T> copyList(List<T> source) {
        return source == null ? null : new ArrayList<>(source);
    }
}
