package com.swiftapp_backend.websocketservice.realtime.application.service;

import com.swiftapp_backend.websocketservice.realtime.application.port.ChatEventPublisher;
import com.swiftapp_backend.websocketservice.realtime.application.port.RealtimeMessagePublisher;
import com.swiftapp_backend.websocketservice.realtime.domain.enums.ContentType;
import com.swiftapp_backend.websocketservice.realtime.domain.enums.MessageType;
import com.swiftapp_backend.websocketservice.realtime.domain.event.MessageCreatedEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ChatEventServiceTest {

    @Test
    void sendMessageFansOutToRedisAndKafka() {
        ChatEventPublisher kafka = mock(ChatEventPublisher.class);
        RealtimeMessagePublisher redis = mock(RealtimeMessagePublisher.class);
        ChatEventService service = new ChatEventService(kafka, redis);
        MessageCreatedEvent event = chatEvent();

        service.sendMessage(event);

        verify(redis).publishChatMessage(event);
        verify(kafka).sendMessageCreated(event);
    }

    @Test
    void replyUpdateAndDeleteUseTheirChannels() {
        ChatEventPublisher kafka = mock(ChatEventPublisher.class);
        RealtimeMessagePublisher redis = mock(RealtimeMessagePublisher.class);
        ChatEventService service = new ChatEventService(kafka, redis);
        MessageCreatedEvent event = chatEvent();

        service.replyMessage(event);
        service.updateMessage(event);
        service.deleteMessage(event);

        verify(kafka).sendMessageReply(event);
        verify(kafka).sendMessageUpdated(event);
        verify(kafka).sendMessageDelete(event);
    }

    @Test
    void rejectsMessagesWithoutClientGeneratedId() {
        ChatEventPublisher kafka = mock(ChatEventPublisher.class);
        RealtimeMessagePublisher redis = mock(RealtimeMessagePublisher.class);
        ChatEventService service = new ChatEventService(kafka, redis);
        MessageCreatedEvent event = chatEvent();
        event.setMessageId("  ");

        assertThrows(IllegalArgumentException.class, () -> service.sendMessage(event));
        verifyNoInteractions(redis);
        verifyNoInteractions(kafka);
    }

    private MessageCreatedEvent chatEvent() {
        return MessageCreatedEvent.builder()
                .messageId("msg-1")
                .sentById(42L)
                .messageContent("hello")
                .sessionId(101L)
                .contentType(ContentType.TEXT)
                .messageType(MessageType.MESSAGE)
                .build();
    }
}
