package com.swiftapp_backend.riderservice.ride.domain.event;

import com.swiftapp_backend.riderservice.ride.domain.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RideCompletedEventDto extends BaseRideEventDto {

    private Double actualDistance;

    private Long actualDuration;

    private BigDecimal totalFare;

    private Currency currency;

    private LocalDateTime completedAt;
}