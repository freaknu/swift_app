package com.swiftapp_backend.chatservice.chat.domain.repository;

import com.swiftapp_backend.chatservice.chat.domain.model.Message;
import java.util.Optional;

public interface MessageRepository {
    void saveMessage(Message message);
    default Optional<Message> findById(String messageId) {
        return Optional.empty();
    }
    void deleteMessage(Long deletedBy, String messageId);
    void updateMessage(Message messages);
}
