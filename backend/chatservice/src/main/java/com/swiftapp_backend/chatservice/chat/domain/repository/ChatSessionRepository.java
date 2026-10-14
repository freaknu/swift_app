package com.swiftapp_backend.chatservice.chat.domain.repository;

import com.swiftapp_backend.chatservice.chat.domain.model.ChatSession;
import com.swiftapp_backend.chatservice.chat.domain.model.Message;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository {
    void saveChatSession(ChatSession session);
    void markInactive(Long sessionId,boolean status);
    Optional<ChatSession> getChatSession(Long rideId);
    /** Non-locking lookup for read-only authorization and history requests. */
    default Optional<ChatSession> findByRideId(Long rideId) {
        return getChatSession(rideId);
    }

    /** Non-locking lookup for read-only authorization and history requests. */
    default Optional<ChatSession> findById(Long sessionId) {
        return Optional.empty();
    }

    default Optional<ChatSession> lockByRideId(Long rideId) {
        return getChatSession(rideId);
    }

    default Optional<ChatSession> lockById(Long sessionId) {
        return Optional.empty();
    }
    List<Message> findAllMessage(Long chatSessionId,int page,int pageSize);
}
