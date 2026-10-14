package com.swiftapp_backend.chatservice.chat.application.service;

import com.swiftapp_backend.chatservice.chat.api.dto.SendMessageRequest;
import com.swiftapp_backend.chatservice.chat.application.port.ChatIdentityPort;
import com.swiftapp_backend.chatservice.chat.application.port.ChatMessagePublisher;
import com.swiftapp_backend.chatservice.chat.domain.enums.ContentType;
import com.swiftapp_backend.chatservice.chat.domain.enums.MessageType;
import com.swiftapp_backend.chatservice.chat.domain.model.ChatSession;
import com.swiftapp_backend.chatservice.chat.domain.model.Message;
import com.swiftapp_backend.chatservice.chat.domain.repository.ChatSessionRepository;
import com.swiftapp_backend.chatservice.chat.domain.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {
    private ChatSessionRepository sessions;
    private MessageRepository messages;
    private ChatIdentityPort identities;
    private ChatMessagePublisher publisher;
    private ChatService service;

    @BeforeEach
    void setUp() {
        sessions = mock(ChatSessionRepository.class);
        messages = mock(MessageRepository.class);
        identities = mock(ChatIdentityPort.class);
        publisher = mock(ChatMessagePublisher.class);
        service = new ChatService(sessions, messages, identities, publisher);
    }

    @Test
    void openingTheSameRideAfterCancellationDoesNotReopenIt() {
        ChatSession closed = session(false, "CANCELLED");
        when(sessions.lockByRideId(101L)).thenReturn(Optional.of(closed));

        ChatSession result = service.openOrUpdateSession(101L, 11L, 42L,
                "DRIVER_ASSIGNED", null, null);

        assertFalse(result.isActive());
        verify(sessions, never()).saveChatSession(any());
    }

    @Test
    void closingAnActiveRideIsIdempotent() {
        ChatSession active = session(true, "STARTED");
        when(sessions.lockByRideId(101L)).thenReturn(Optional.of(active));

        service.closeSessionForRide(101L, "COMPLETED");
        service.closeSessionForRide(101L, "COMPLETED");

        assertFalse(active.isActive());
        assertEquals("COMPLETED", active.getRideStatus());
        verify(sessions).saveChatSession(active);
    }

    @Test
    void authorizedMessageIsPersistedAndPublishedWithProfileSender() {
        ChatSession active = session(true, "STARTED");
        when(sessions.lockById(7L)).thenReturn(Optional.of(active));
        when(identities.identityForAuthUser(42L))
                .thenReturn(new ChatIdentityPort.Identity(42L, 42L, null));
        when(messages.findById("m-1")).thenReturn(Optional.empty());

        Message sent = service.sendMessage(42L, 7L, SendMessageRequest.builder()
                .messageId("m-1")
                .messageContent("On my way")
                .contentType(ContentType.TEXT)
                .build(), MessageType.MESSAGE, null);

        assertEquals(42L, sent.getSentById());
        assertEquals(7L, sent.getSessionId());
        verify(messages).saveMessage(sent);
        verify(publisher).publish(sent, active);
    }

    @Test
    void inactiveSessionRejectsMessages() {
        when(sessions.lockById(7L)).thenReturn(Optional.of(session(false, "COMPLETED")));
        when(identities.identityForAuthUser(42L))
                .thenReturn(new ChatIdentityPort.Identity(42L, 42L, null));

        assertThrows(ChatSessionStateException.class, () -> service.sendMessage(
                42L, 7L, SendMessageRequest.builder().messageId("m-2")
                        .messageContent("late").build(), MessageType.MESSAGE, null));
        verify(messages, never()).saveMessage(any());
        verify(publisher, never()).publish(any(), any());
    }

    @Test
    void authAndProfileNumericCoincidenceDoesNotAuthorizeAnotherChat() {
        ChatSession active = session(true, "STARTED");
        when(sessions.lockById(7L)).thenReturn(Optional.of(active));
        when(identities.identityForAuthUser(42L))
                .thenReturn(new ChatIdentityPort.Identity(42L, 900L, 901L));

        assertThrows(ChatAuthorizationException.class, () -> service.sendMessage(
                42L, 7L, SendMessageRequest.builder().messageId("m-3")
                        .messageContent("not allowed").build(), MessageType.MESSAGE, null));
        verify(messages, never()).saveMessage(any());
    }

    @Test
    void readOnlySessionLookupDoesNotRequireAPessimisticLock() {
        ChatSession closed = session(false, "COMPLETED");
        when(sessions.findByRideId(101L)).thenReturn(Optional.of(closed));

        assertEquals(closed, service.getSessionByRideId(101L).orElseThrow());
        verify(sessions, never()).lockByRideId(101L);
    }

    @Test
    void readOnlyParticipantCheckDoesNotRequireAPessimisticLock() {
        ChatSession active = session(true, "STARTED");
        when(sessions.findById(7L)).thenReturn(Optional.of(active));
        when(identities.identityForAuthUser(42L))
                .thenReturn(new ChatIdentityPort.Identity(42L, 42L, null));

        service.requireParticipant(42L, 7L);

        verify(sessions, never()).lockById(7L);
    }

    private ChatSession session(boolean active, String status) {
        return ChatSession.builder()
                .sessionId(7L)
                .rideId(101L)
                .riderId(42L)
                .driverId(11L)
                .rideStatus(status)
                .active(active)
                .build();
    }
}
