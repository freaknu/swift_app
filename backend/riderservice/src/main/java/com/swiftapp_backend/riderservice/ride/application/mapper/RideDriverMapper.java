package com.swiftapp_backend.riderservice.ride.application.mapper;


import com.swiftapp_backend.riderservice.ride.domain.event.RideDriverAcceptResponseEvent;
import com.swiftapp_backend.riderservice.ride.api.dto.ridedriver.RideDriverResponseDto;
import com.swiftapp_backend.riderservice.ride.domain.model.RideDriverModel;

public class RideDriverMapper {
    public static RideDriverModel toModelFromModel(RideDriverResponseDto data) {
        return RideDriverModel
                .builder()
                .authUserId(data.getAuthUserId())
                .driverEmail(data.getDriverEmail())
                .driverPhone(data.getDriverPhone())
                .driverId(data.getDriverId())
                .driverName(data.getDriverName())
                .driverGender(data.getDriverGender())
                .driverProfileImage(data.getDriverProfileImage())
                .build();
    }

    public static RideDriverModel toModel(RideDriverAcceptResponseEvent data) {
        return RideDriverModel
                .builder()
                .driverName(data.getDriverName())
                .driverEmail(data.getDriverEmail())
                .driverId(data.getDriverId())
                .driverPhone(data.getDriverPhone())
                .driverGender(data.getDriverGender())
                .authUserId(data.getAuthUserId())
                .isActive(data.getIsActive())
                .driverProfileImage(data.getDriverProfileImage())
                .build();
    }
}
