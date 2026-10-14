package com.swiftapp_backend.matchingservice.matching.domain.models;

import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class RideRequest {
    private double latitude;
    private double longitude;
    private double radiusInKms;
    private VehicleTypeEnum vehicleTypeEnum;
}
