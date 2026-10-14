package com.swiftapp_backend.matchingservice.matching.domain.events;

import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class RideRequestEvent {
    private double latitude;
    private double longitude;
    private double radiusInKms;
    private VehicleTypeEnum vehicleTypeEnum;
}
