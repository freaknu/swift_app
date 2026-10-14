package com.swiftapp_backend.riderservice.ride.domain.event;

import com.swiftapp_backend.riderservice.ride.domain.enums.Currency;
import com.swiftapp_backend.riderservice.ride.domain.enums.RideType;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestedEventDto extends BaseRideEventDto {

    private RideType rideType;

    /** Matching-service vehicle category. */
    private String vehicleType;

    private Double pickupLatitude;

    private Double pickupLongitude;

    private Double destinationLatitude;

    private Double destinationLongitude;

    private Double estimatedDistance;

    private Long estimatedDuration;

    private BigDecimal baseFare;

    private BigDecimal distanceFare;

    private BigDecimal timeFare;

    private BigDecimal bookingFee;

    private BigDecimal tax;

    private BigDecimal surgeMultiplier;

    private BigDecimal discount;

    private BigDecimal totalFare;

    private Currency currency;
}
