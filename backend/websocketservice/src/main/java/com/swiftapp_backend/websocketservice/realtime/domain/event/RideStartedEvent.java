package com.swiftapp_backend.websocketservice.realtime.domain.event;

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
public class RideStartedEvent extends BaseRideEvent {

    private LocalDateTime startedAt;
}
