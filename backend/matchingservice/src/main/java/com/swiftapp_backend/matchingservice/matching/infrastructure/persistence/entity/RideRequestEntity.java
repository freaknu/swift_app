package com.swiftapp_backend.matchingservice.matching.infrastructure.persistence.entity;

import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import lombok.*;

@Getter
@Setter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class RideRequestEntity {
    private double latitude;
    private double longitude;
    private double radiusInKms;
    private VehicleTypeEnum vehicleTypeEnum;
}
