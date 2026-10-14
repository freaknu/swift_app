package com.swiftapp_backend.websocketservice.realtime.application.port;

import com.swiftapp_backend.websocketservice.realtime.domain.event.ChatSessionCreateEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.MessageCreatedEvent;

public interface ChatEventPublisher {
    void sendMessageCreated(MessageCreatedEvent event);
    void sendMessageReply(MessageCreatedEvent event);
    void sendMessageUpdated(MessageCreatedEvent event);
    void sendMessageDelete(MessageCreatedEvent event);
    void sendSessionCreated(ChatSessionCreateEvent event);
}
