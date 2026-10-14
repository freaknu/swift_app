package com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.math.BigDecimal;

/**
 * Local mirror of the matching service's {@code ride.offer.broadcast}
 * event. Unknown fields are ignored so the matching side can evolve
 * without breaking this consumer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RideOfferBroadcastPayload {

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

    private String vehicleType;

    private int attempt;

    private double radiusKm;

    private String expiresAt;

    private List<OfferTarget> targets;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OfferTarget {

        private Long driverId;

        private double distanceKm;
    }
}
