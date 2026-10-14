package com.swiftapp_backend.websocketservice.realtime.domain.event;

import com.swiftapp_backend.websocketservice.realtime.domain.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestedEvent extends BaseRideEvent {

    private Long pickupAddressId;

    private Long destinationAddressId;

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
