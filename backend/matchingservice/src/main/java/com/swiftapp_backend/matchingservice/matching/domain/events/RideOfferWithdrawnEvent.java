package com.swiftapp_backend.matchingservice.matching.domain.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Published on {@code ride.offer.withdrawn} when a ride's offers stop being
 * actionable for the listed drivers: another driver accepted first, or the
 * offer window expired and the search moved to a wider radius. The websocket
 * service pushes this so driver apps can dismiss the offer card, and the
 * driver service drops the staged pending offer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideOfferWithdrawnEvent {

    private Long rideId;

    private List<Long> driverIds;

    /** ACCEPTED_BY_OTHER_DRIVER or EXPIRED. */
    private String reason;
}
