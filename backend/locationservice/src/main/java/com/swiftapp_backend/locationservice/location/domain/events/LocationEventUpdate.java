package com.swiftapp_backend.locationservice.location.domain.events;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class LocationEventUpdate{
    private Long driverId;
    private double latitude;
    private double longitude;
    private String driverStatus;
    private String vehicleTypeEnum;
}
