package com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Small provider-neutral subset shared by ride lifecycle consumers. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideLifecycleEvent {
    private Long rideId;
    private Long riderId;
    private Long driverId;
    private String rideStatus;
}
