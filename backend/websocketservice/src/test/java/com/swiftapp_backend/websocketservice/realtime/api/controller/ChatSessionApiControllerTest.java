package com.swiftapp_backend.websocketservice.realtime.api.controller;

import com.swiftapp_backend.websocketservice.realtime.api.dto.ChatSessionDto;
import com.swiftapp_backend.websocketservice.realtime.application.service.ChatEventService;
import com.swiftapp_backend.websocketservice.realtime.domain.event.ChatSessionCreateEvent;
import com.swiftapp_backend.websocketservice.realtime.infrastructure.config.RideChatSessionClient;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatSessionApiControllerTest {
    private final ChatEventService events = mock(ChatEventService.class);
    private final RideChatSessionClient rides = mock(RideChatSessionClient.class);
    private final ChatSessionApiController controller = new ChatSessionApiController(events, rides);

    @Test
    void publishesOnlyTheValidatedRideContext() {
        ChatSessionDto request = ChatSessionDto.builder().rideId(101L)
                .driverId(11L).riderId(201L).rideStatus("COMPLETED").build();
        when(rides.validate(42L, request)).thenReturn(
                new RideChatSessionClient.Context(101L, 201L, 11L, "STARTED"));

        assertEquals(HttpStatus.ACCEPTED, controller.createSession(42L, request).getStatusCode());

        ArgumentCaptor<ChatSessionCreateEvent> event = ArgumentCaptor.forClass(ChatSessionCreateEvent.class);
        verify(events).createSession(event.capture());
        assertEquals("STARTED", event.getValue().getRideStatus());
        assertEquals(201L, event.getValue().getRiderId());
    }

    @Test
    void rejectedBootstrapCannotPublishAParticipantReplacement() {
        ChatSessionDto request = new ChatSessionDto();
        when(rides.validate(42L, request)).thenThrow(
                new ResponseStatusException(HttpStatus.FORBIDDEN, "Only participants can bootstrap chat"));

        assertThrows(ResponseStatusException.class, () -> controller.createSession(42L, request));
        verifyNoInteractions(events);
    }
}
