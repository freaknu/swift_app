package com.swiftapp_backend.chatservice.chat.application.port;

import com.swiftapp_backend.chatservice.chat.domain.model.Message;
import com.swiftapp_backend.chatservice.chat.domain.model.ChatSession;

public interface ChatMessagePublisher {
    void publish(Message message, ChatSession session);
}
