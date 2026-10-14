package com.swiftapp_backend.locationservice.location.api.dto.event;

import com.swiftapp_backend.locationservice.location.domain.enums.Direction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class LocationUpdateDto {
    /** Relay metadata is intentionally transport-only and is not persisted. */
    private String eventId;
    private Instant eventTime;
    private String vehicleType;
    private String driverStatus;
    private Long id;
    private Long driverId;
    private Long rideId;
    private Long riderId;
    private double latitude;
    private double longitude;
    private String nearByPlace;
    private Direction direction;
    private Double distanceToPickupMeters;
    private Long etaMinutes;
    private Boolean driverArrived;
    private Double distanceToDestinationMeters;
    private Long destinationEtaMinutes;
}
