package com.swiftapp_backend.driverservice.driver.application.mapper;

import com.swiftapp_backend.driverservice.driver.api.dto.VehicleCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.api.dto.VehicleResponseDto;
import com.swiftapp_backend.driverservice.driver.domain.model.VehicleModel;

public class VehicleMapper {
    public static VehicleModel toModel(VehicleCreateRequestDto data) {
        return VehicleModel
                .builder()
                .vehicleType(data.getVehicleType())
                .brand(data.getBrand())
                .color(data.getColor())
                .fuelType(data.getFuelType())
                .isEv(Boolean.TRUE.equals(data.getIsEv()))
                .manufactureYear(data.getManufactureYear() == null ? 0 : data.getManufactureYear())
                .registrationNumber(data.getRegistrationNumber())
                .seatCapacity(data.getSeatCapacity() == null ? 0 : data.getSeatCapacity())
                .model(data.getModel())
                .year(data.getYear() == null ? 0 : data.getYear())
                .seats(data.getSeats() == null ? 0 : data.getSeats())
                .vehicleImage(data.getVehicleImage())
                .build();
    }

    public static VehicleResponseDto toVehicleResponse(VehicleModel data) {

        VehicleResponseDto res = new VehicleResponseDto();

        res.setId(data.getId());
        res.setVehicleType(data.getVehicleType());
        res.setAuthUserId(data.getAuthUserId());

        if (data.getDriver() != null) {
            res.setDriverId(data.getDriver().getId());
            if (data.getAuthUserId() == null) {
                res.setAuthUserId(data.getDriver().getAuthUserId());
            }
        }

        res.setBrand(data.getBrand());
        res.setModel(data.getModel());
        res.setYear(data.getYear());
        res.setRegistrationNumber(data.getRegistrationNumber());
        res.setFuelType(data.getFuelType());
        res.setColor(data.getColor());
        res.setSeatCapacity(data.getSeatCapacity());
        res.setActive(data.isActive());
        res.setIsVerified(data.getIsVerified());
        res.setEv(data.isEv());
        res.setManufactureYear(data.getManufactureYear());
        res.setSeats(data.getSeats());
        res.setVehicleImage(data.getVehicleImage());
        res.setCreatedAt(data.getCreatedAt());
        res.setUpdatedAt(data.getUpdatedAt());

        return res;
    }
}
