package com.swiftapp_backend.websocketservice.realtime.application.service;

import com.swiftapp_backend.websocketservice.realtime.application.port.ChatEventPublisher;
import com.swiftapp_backend.websocketservice.realtime.application.port.RealtimeMessagePublisher;
import com.swiftapp_backend.websocketservice.realtime.domain.event.ChatSessionCreateEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.MessageCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;


@Service
@RequiredArgsConstructor
public class ChatEventService {
    private final ChatEventPublisher chatEventPublisher;
    private final RealtimeMessagePublisher realtimeMessagePublisher;

    @Async
    public void sendMessage(MessageCreatedEvent event) {
        publish(event, chatEventPublisher::sendMessageCreated);
    }

    @Async
    public void replyMessage(MessageCreatedEvent event) {
        publish(event, chatEventPublisher::sendMessageReply);
    }

    @Async
    public void updateMessage(MessageCreatedEvent event) {
        publish(event, chatEventPublisher::sendMessageUpdated);
    }

    @Async
    public void deleteMessage(MessageCreatedEvent event) {
        publish(event, chatEventPublisher::sendMessageDelete);
    }

    @Async
    public void createSession(ChatSessionCreateEvent event) {
        chatEventPublisher.sendSessionCreated(event);
    }

    private void publish(MessageCreatedEvent event, Consumer<MessageCreatedEvent> kafkaPublish) {
        requireClientMessageId(event);
        realtimeMessagePublisher.publishChatMessage(event);
        kafkaPublish.accept(event);
    }

    private void requireClientMessageId(MessageCreatedEvent event) {
        if (event.getMessageId() == null || event.getMessageId().isBlank()) {
            throw new IllegalArgumentException("messageId must be generated on the client (Flutter) side");
        }
    }
}
