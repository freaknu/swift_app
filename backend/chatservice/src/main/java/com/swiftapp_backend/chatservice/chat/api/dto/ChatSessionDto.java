package com.swiftapp_backend.chatservice.chat.api.dto;

import com.swiftapp_backend.chatservice.chat.domain.model.ChatSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSessionDto {
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

    public static ChatSessionDto fromDomain(ChatSession session) {
        return ChatSessionDto.builder()
                .sessionId(session.getSessionId())
                .sessionNameForDriver(session.getSessionNameForDriver())
                .sessionNameForRider(session.getSessionNameForRider())
                .driverId(session.getDriverId())
                .riderId(session.getRiderId())
                .rideId(session.getRideId())
                .rideStatus(session.getRideStatus())
                .active(session.isActive())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
