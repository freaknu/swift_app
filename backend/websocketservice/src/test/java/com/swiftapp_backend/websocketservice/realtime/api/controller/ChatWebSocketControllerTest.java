package com.swiftapp_backend.websocketservice.realtime.api.controller;

import com.swiftapp_backend.websocketservice.realtime.api.dto.ChatMessageDto;
import com.swiftapp_backend.websocketservice.realtime.infrastructure.config.ChatMessageWriteClient;
import com.swiftapp_backend.websocketservice.realtime.infrastructure.config.DriverSubscriptionInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ChatWebSocketControllerTest {
    private final ChatMessageWriteClient writes = mock(ChatMessageWriteClient.class);
    private final ChatWebSocketController controller = new ChatWebSocketController(writes);

    @Test
    void sendAndReplyUseTheValidatedSocketIdentityNotClientHeaders() {
        ChatMessageDto message = ChatMessageDto.builder().messageId("m-1")
                .sentById(999L).sessionId(7L).messageContent("test").build();
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionAttributes(Map.of(DriverSubscriptionInterceptor.AUTH_USER_ID_ATTRIBUTE, 42L));

        controller.send(message, headers);
        controller.reply(message, headers);

        verify(writes).send(42L, message, false);
        verify(writes).send(42L, message, true);
    }

    @Test
    void missingSocketIdentityCannotWrite() {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        assertThrows(MessagingException.class, () -> controller.send(new ChatMessageDto(), headers));
        verifyNoInteractions(writes);
    }

    @Test
    void oldEditAndDeleteHandlersCannotBypassTheDurableBoundary() {
        assertThrows(MessagingException.class, () -> controller.update(new ChatMessageDto()));
        assertThrows(MessagingException.class, () -> controller.delete(new ChatMessageDto()));
        verifyNoInteractions(writes);
    }

    @Test
    void writeErrorsDoNotExposeUpstreamResponseOrIdentityDetails() {
        assertEquals(Map.of("error", "CHAT_WRITE_REJECTED"), controller.rejectedWrite(
                new MessagingException("sensitive upstream details")));
    }
}
