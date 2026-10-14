package com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ChatSessionCreateEvent {
    private String sessionNameForDriver;
    private String sessionNameForRider;
    private Long driverId;
    private Long riderId;
    private Long rideId;
    private String rideStatus;
}
