package com.swiftapp_backend.driverservice.driver.application.mapper;


import com.swiftapp_backend.driverservice.driver.api.dto.DriverPreferenceCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverPreferenceResponseDto;
import com.swiftapp_backend.driverservice.driver.domain.model.DriverPreferenceModel;

public class DriverPreferenceMapper {
    public static DriverPreferenceModel toModel(DriverPreferenceCreateRequestDto data) {
        return DriverPreferenceModel
                .builder()
                .acceptCash(data.getAcceptCash())
                .acceptOnline(data.getAcceptOnline())
                .preferredRadius(data.getPreferredRadius())
                .build();
    }


    public static DriverPreferenceResponseDto toResponseDto(DriverPreferenceModel data) {
        return DriverPreferenceResponseDto
                .builder()
                .acceptCash(data.isAcceptCash())
                .autoAccept(data.isAutoAccept())
                .acceptOnline(data.isAcceptOnline())
                .preferredRadius(data.getPreferredRadius())
                .build();
    }
}
