package com.swiftapp_backend.riderservice.ride.api.dto.fare;

import com.swiftapp_backend.riderservice.ride.domain.enums.Currency;
import com.swiftapp_backend.riderservice.ride.domain.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class FareDto {
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

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
