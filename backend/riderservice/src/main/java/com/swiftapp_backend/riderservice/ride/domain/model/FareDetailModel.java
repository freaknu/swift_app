package com.swiftapp_backend.riderservice.ride.domain.model;

import com.swiftapp_backend.riderservice.ride.domain.enums.Currency;
import com.swiftapp_backend.riderservice.ride.domain.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareDetailModel {

    private Long id;

    private BigDecimal baseFare;

    private BigDecimal distanceFare;

    private BigDecimal timeFare;

    private BigDecimal bookingFee;

    private BigDecimal tax;

    private BigDecimal surgeMultiplier;

    private BigDecimal discount;

    private BigDecimal totalFare;

    private Currency currency;

    private PaymentStatus paymentStatus;

    private RideModel ride;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
