package com.swiftapp_backend.chatservice.chat.domain.model;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ChatSession {
    private Long sessionId;
    private String sessionNameForDriver;
    private String sessionNameForRider;
    private Long driverId;
    private Long riderId;
    private Long rideId;
    private String rideStatus;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
