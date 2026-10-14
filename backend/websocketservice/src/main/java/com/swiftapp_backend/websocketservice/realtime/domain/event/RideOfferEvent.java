package com.swiftapp_backend.websocketservice.realtime.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Pushed to {@code /topic/drivers/{driverId}} when the matching service
 * broadcasts a ride to the drivers inside the current search radius.
 * rideStatus is OFFER_SENT.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RideOfferEvent extends BaseRideEvent {

    private Double pickupLatitude;

    private Double pickupLongitude;

    private Double destinationLatitude;

    private Double destinationLongitude;

    private Double estimatedDistance;

    private Long estimatedDuration;

    private java.math.BigDecimal totalFare;

    private String currency;

    /** Canonical requested vehicle type, shared with the driver offer inbox. */
    private String rideTypeCode;

    /** This driver's distance from the pickup point, in kilometres. */
    private Double distanceKm;

    /** 1-based matching attempt; the search radius grows with the attempt. */
    private Integer attempt;

    /** Search radius used for this broadcast, in kilometres. */
    private Double radiusKm;

    /** ISO-8601 instant when the offer expires. */
    private String expiresAt;
}
