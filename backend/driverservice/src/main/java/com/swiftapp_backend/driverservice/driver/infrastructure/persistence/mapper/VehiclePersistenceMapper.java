package com.swiftapp_backend.driverservice.driver.infrastructure.persistence.mapper;

import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;
import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.DriverEntity;
import com.swiftapp_backend.driverservice.driver.infrastructure.persistence.entity.VehicleEntity;

import java.util.ArrayList;

public final class VehiclePersistenceMapper {
    private VehiclePersistenceMapper() {
    }

    public static VehicleModel toDomain(VehicleEntity source) {
        if (source == null) {
            return null;
        }
        return toDomain(source, DriverPersistenceMapper.toDomainReference(source.getDriver()));
    }

    static VehicleModel toDomain(VehicleEntity source, DriverModel driver) {
        if (source == null) {
            return null;
        }

        VehicleModel target = new VehicleModel();
        target.setId(source.getId());
        target.setDriver(driver);
        target.setVehicleType(source.getVehicleType());
        target.setAuthUserId(source.getAuthUserId());
        target.setBrand(source.getBrand());
        target.setModel(source.getModel());
        target.setYear(source.getYear());
        target.setRegistrationNumber(source.getRegistrationNumber());
        target.setFuelType(source.getFuelType());
        target.setColor(source.getColor());
        target.setSeatCapacity(source.getSeatCapacity());
        target.setActive(source.isActive());
        target.setEv(source.isEv());
        target.setManufactureYear(source.getManufactureYear());
        target.setSeats(source.getSeats());
        target.setIsVerified(source.getIsVerified());
        target.setVehicleImage(source.getVehicleImage() == null
                ? null
                : new ArrayList<>(source.getVehicleImage()));
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        return target;
    }

    public static VehicleEntity toEntity(VehicleModel source, DriverEntity driver) {
        if (source == null) {
            return null;
        }

        VehicleEntity target = new VehicleEntity();
        target.setId(source.getId());
        updateEntity(source, target, driver);
        return target;
    }

    public static void updateEntity(VehicleModel source, VehicleEntity target, DriverEntity driver) {
        target.setDriver(driver);
        target.setVehicleType(source.getVehicleType());
        target.setAuthUserId(source.getAuthUserId());
        target.setBrand(source.getBrand());
        target.setModel(source.getModel());
        target.setYear(source.getYear());
        target.setRegistrationNumber(source.getRegistrationNumber());
        target.setFuelType(source.getFuelType());
        target.setColor(source.getColor());
        target.setSeatCapacity(source.getSeatCapacity());
        target.setActive(source.isActive());
        target.setEv(source.isEv());
        target.setManufactureYear(source.getManufactureYear());
        target.setSeats(source.getSeats());
        target.setIsVerified(source.getIsVerified());
        target.setCreatedAt(source.getCreatedAt());
        target.setUpdatedAt(source.getUpdatedAt());
        if (source.getVehicleImage() != null) {
            if (target.getVehicleImage() == null) {
                target.setVehicleImage(new ArrayList<>(source.getVehicleImage()));
            } else {
                target.getVehicleImage().clear();
                target.getVehicleImage().addAll(source.getVehicleImage());
            }
        }
    }
}
