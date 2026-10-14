package com.swiftapp_backend.driverservice.driver.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A ride offer waiting for the driver's decision. Stored in Redis when
 * {@code ride.offer.broadcast} arrives from the matching service and removed
 * once the driver accepts, rejects, or the offer expires.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingOfferDto {

    private Long rideId;

    private Long riderId;

    private Long driverId;

    private double distanceKm;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private Double destinationLatitude;

    private Double destinationLongitude;

    private Double estimatedDistance;

    private Long estimatedDuration;

    private java.math.BigDecimal totalFare;

    private String currency;

    /** Canonical vehicle type requested by the rider, for example SEDAN. */
    private String rideTypeCode;

    /** ISO-8601 instant when the matching-service offer window closes. */
    private String expiresAt;

    /** Current server time when the offer inbox response is sent. */
    private String serverTime;
}
