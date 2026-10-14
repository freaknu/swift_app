package com.swiftapp_backend.websocketservice.realtime.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Pushed to {@code /topic/drivers/{driverId}} when a ride's offer stops
 * being actionable for that driver: another driver accepted first, or the
 * offer window expired and the search moved to a wider radius.
 * rideStatus is OFFER_WITHDRAWN.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RideOfferWithdrawnEvent extends BaseRideEvent {

    /** ACCEPTED_BY_OTHER_DRIVER or EXPIRED. */
    private String reason;
}
