package com.swiftapp_backend.websocketservice.realtime.domain.model;

import com.swiftapp_backend.websocketservice.realtime.domain.enums.Direction;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LocationUpdate {
    Long id;
    Long driverId;
    Long rideId;
    Long riderId;
    double latitude;
    double longitude;
    String nearByPlace;
    Direction direction;
}
