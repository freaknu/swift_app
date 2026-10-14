package com.swiftapp_backend.chatservice.chat.infrastructure.persistence.mapper;

import com.swiftapp_backend.chatservice.chat.domain.model.ChatSession;
import com.swiftapp_backend.chatservice.chat.infrastructure.persistence.entity.ChatSessionEntity;

public class ChatSessionPersistenceMapper {
    public static ChatSession toDomain(ChatSessionEntity entity) {
        return ChatSession
                .builder()
                .sessionId(entity.getId())
                .sessionNameForDriver(entity.getSessionNameForDriver())
                .sessionNameForRider(entity.getSessionNameForRider())
                .driverId(entity.getDriverId())
                .riderId(entity.getRiderId())
                .rideId(entity.getRideId())
                .rideStatus(entity.getRideStatus())
                .active(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt()).build();
    }

    public static ChatSessionEntity toEntity(ChatSession domain) {
        return ChatSessionEntity
                .builder()
                .id(domain.getSessionId())
                .sessionNameForDriver(domain.getSessionNameForDriver())
                .sessionNameForRider(domain.getSessionNameForRider())
                .driverId(domain.getDriverId())
                .riderId(domain.getRiderId())
                .rideId(domain.getRideId())
                .rideStatus(domain.getRideStatus())
                .isActive(domain.isActive())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
