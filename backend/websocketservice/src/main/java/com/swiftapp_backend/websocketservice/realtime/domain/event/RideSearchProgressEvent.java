package com.swiftapp_backend.websocketservice.realtime.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Pushed to {@code /topic/rides/{rideId}} and
 * {@code /topic/riders/{riderId}} on every matching broadcast so the
 * rider app can show the live expanding search ("searching within X km").
 * rideStatus is SEARCHING.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RideSearchProgressEvent extends BaseRideEvent {

    /** 1-based matching attempt. */
    private Integer attempt;

    /** Search radius used for this broadcast, in kilometres. */
    private Double radiusKm;

    /** How many drivers were targeted by this broadcast. */
    private Integer driverCount;
}
