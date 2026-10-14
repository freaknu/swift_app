package com.swiftapp_backend.matchingservice.matching.domain.events;

import com.swiftapp_backend.matchingservice.matching.domain.enums.VehicleTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

/**
 * Internal representation of a ride.requested event.
 * Mapped from the riderservice RideRequestedEventDto JSON
 * (unknown fields are ignored).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RideRequestedEvent {

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
}
