package com.swiftapp_backend.matchingservice.matching.domain.events;

import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverLocationEvent {
    private Long driverId;
    private double latitude;
    private double longitude;
    private DriverStatusEventEnum driverStatus;
    private VehicleTypeEnum vehicleTypeEnum;
}
