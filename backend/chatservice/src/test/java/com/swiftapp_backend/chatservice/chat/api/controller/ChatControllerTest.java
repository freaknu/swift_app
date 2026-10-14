package com.swiftapp_backend.chatservice.chat.api.controller;

import com.swiftapp_backend.chatservice.chat.api.dto.SendMessageRequest;
import com.swiftapp_backend.chatservice.chat.application.service.ChatService;
import com.swiftapp_backend.chatservice.chat.domain.enums.MessageType;
import com.swiftapp_backend.chatservice.chat.domain.model.Message;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatControllerTest {
    @Test
    void restMessageWriteUsesGatewayIdentityAndReturnsCreatedMessage() {
        ChatService service = mock(ChatService.class);
        ChatController controller = new ChatController(service);
        SendMessageRequest request = SendMessageRequest.builder()
                .messageId("m-1")
                .messageContent("I am outside")
                .build();
        Message saved = Message.builder()
                .messageId("m-1")
                .sentById(42L)
                .sessionId(7L)
                .messageContent("I am outside")
                .messageType(MessageType.MESSAGE)
                .build();
        when(service.sendMessage(42L, 7L, request, MessageType.MESSAGE, null))
                .thenReturn(saved);

        ResponseEntity<?> response = controller.sendMessage(42L, 7L, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(service).sendMessage(42L, 7L, request, MessageType.MESSAGE, null);
    }

    @Test
    void websocketAccessProbeReturnsNoContentAfterParticipantCheck() {
        ChatService service = mock(ChatService.class);
        ChatController controller = new ChatController(service);

        ResponseEntity<Void> response = controller.checkSessionAccess(42L, 7L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service).requireParticipant(42L, 7L);
    }
}
