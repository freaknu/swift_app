package com.swiftapp_backend.websocketservice.realtime.domain.event;


import com.swiftapp_backend.websocketservice.realtime.domain.enums.ChangedByType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RideCancelledEvent extends BaseRideEvent {

    private Long cancelledBy;

    private ChangedByType cancelledByType;

    private String reason;

    private LocalDateTime cancelledAt;
}
