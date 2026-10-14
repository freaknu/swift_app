package com.swiftapp_backend.riderservice.ride.domain.event;

import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseRideEventDto {

    private Long rideId;

    private Long riderId;

    private Long driverId;

    private Long vehicleId;

    private RideStatus rideStatus;

    private LocalDateTime eventTime;
}