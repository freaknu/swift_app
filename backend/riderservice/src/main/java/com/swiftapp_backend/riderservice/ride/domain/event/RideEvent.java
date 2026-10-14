package com.swiftapp_backend.riderservice.ride.domain.event;

import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import com.swiftapp_backend.riderservice.ride.domain.model.RideModel;

import java.time.OffsetDateTime;

public record RideEvent(
        Long rideId,
        Long riderId,
        Long driverId,
        Long vehicleId,
        RideStatus status,
        Double pickupLatitude,
        Double pickupLongitude,
        Double destinationLatitude,
        Double destinationLongitude,
        OffsetDateTime occurredAt
) {
    public static RideEvent from(RideModel ride) {
        return new RideEvent(ride.getId(), ride.getRiderId(), ride.getDriverId(), ride.getVehicleId(),
                ride.getCurrentStatus(), ride.getPickupLatitude(), ride.getPickupLongitude(),
                ride.getDestinationLatitude(), ride.getDestinationLongitude(), OffsetDateTime.now());
    }
}
