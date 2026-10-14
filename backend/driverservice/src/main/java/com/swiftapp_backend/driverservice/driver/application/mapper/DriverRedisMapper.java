package com.swiftapp_backend.driverservice.driver.application.mapper;


import com.swiftapp_backend.driverservice.driver.api.dto.DriverSmallDetailDto;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverModel;

public class DriverRedisMapper {

    public static DriverSmallDetailDto toDriverSmallDto(DriverModel driverModel) {
        return DriverSmallDetailDto
                .builder()
                .driverId(driverModel.getId())
                .driverStatus(driverModel.getDriverStatus())
                .isVerified(driverModel.isVerified())
                .build();
    }
}
