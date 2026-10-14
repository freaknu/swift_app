package com.swiftapp_backend.riderservice.ride.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class RideDriverUserFetchRequestEvent {
    private Long rideId;
    private Long driverId;
    private Long userId;
    private double sourceLatitude;
    private double sourceLongitude;
    private double destinationLatitude;
    private double destinationLongitude;
    private Long startAddressId;
    private Long endAddressId;
}
