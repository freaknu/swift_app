package com.swiftapp_backend.matchingservice.matching.domain.dto;

import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverFindDto {

    private double latitude;

    private double longitude;

    private double radiusInKilometers;

    private long limit;

    private VehicleTypeEnum vehicleTypeEnum;

    private DriverStatusEventEnum driverStatus;
}