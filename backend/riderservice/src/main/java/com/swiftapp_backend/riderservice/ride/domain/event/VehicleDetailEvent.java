package com.swiftapp_backend.riderservice.ride.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class VehicleDetailEvent {
    private Long rideId;
    private Long driverId;
    private Long vehicleId;
    private String vehicleNumber;
}
