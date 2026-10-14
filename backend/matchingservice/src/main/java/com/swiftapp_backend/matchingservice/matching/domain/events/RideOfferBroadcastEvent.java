package com.swiftapp_backend.matchingservice.matching.domain.events;

import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.math.BigDecimal;

/**
 * Published on {@code ride.offer.broadcast} when a matching attempt
 * offers a ride to one selected driver inside the current search radius.
 *
 * <p>The websocket service pushes this to the selected driver's channel;
 * the driver service stages it in the pending-offer inbox. A rejection or
 * timeout advances matching to the next untried candidate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideOfferBroadcastEvent {

    private Long rideId;

    private Long riderId;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private Double destinationLatitude;

    private Double destinationLongitude;

    private Double estimatedDistance;

    private Long estimatedDuration;

    private BigDecimal totalFare;

    private String currency;

    private VehicleTypeEnum vehicleType;

    /** 1-based matching attempt; the search radius grows with the attempt. */
    private int attempt;

    /** Search radius used for this broadcast, in kilometres. */
    private double radiusKm;

    /** ISO-8601 instant when every offer in this broadcast expires. */
    private String expiresAt;

    /** The selected driver for this attempt. */
    private List<OfferTarget> targets;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OfferTarget {

        private Long driverId;

        /** Driver distance from the pickup point, in kilometres. */
        private double distanceKm;
    }
}
