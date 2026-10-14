package com.swiftapp_backend.websocketservice.realtime.domain.event;

import com.swiftapp_backend.websocketservice.realtime.domain.enums.Currency;
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
public class RideCompletedEvent extends BaseRideEvent {

    private Double actualDistance;

    private Long actualDuration;

    private BigDecimal totalFare;

    private Currency currency;

    private LocalDateTime completedAt;
}
