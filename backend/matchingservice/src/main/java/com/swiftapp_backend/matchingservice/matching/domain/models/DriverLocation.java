package com.swiftapp_backend.matchingservice.matching.domain.models;

import com.swiftapp_backend.matchingservice.matching.domain.enums.DriverStatusEventEnum;
import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class DriverLocation {
    private Long driverId;
    private double latitude;
    private double longitude;
    private DriverStatusEventEnum statusEvent;
    private VehicleTypeEnum vehicleType;
}
