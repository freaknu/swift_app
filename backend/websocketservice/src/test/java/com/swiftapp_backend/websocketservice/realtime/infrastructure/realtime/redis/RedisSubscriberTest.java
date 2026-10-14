package com.swiftapp_backend.websocketservice.realtime.infrastructure.realtime.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class RedisSubscriberTest {

    @Test
    void routesRideUpdatesToRideDestination() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RedisSubscriber subscriber = new RedisSubscriber(messagingTemplate, new ObjectMapper());
        Map<String, Object> message = Map.of("rideId", "101", "driverId", 11L);

        subscriber.receiveMessage(message);

        verify(messagingTemplate).convertAndSend("/topic/rides/101", message);
    }

    @Test
    void routesDriverUpdatesToDriverDestination() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RedisSubscriber subscriber = new RedisSubscriber(messagingTemplate, new ObjectMapper());
        Map<String, Object> message = Map.of("driverId", 11L);

        subscriber.receiveMessage(message);

        verify(messagingTemplate).convertAndSend("/topic/drivers/11", message);
    }

    @Test
    void routesUnidentifiedUpdatesToGeneralRideDestination() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RedisSubscriber subscriber = new RedisSubscriber(messagingTemplate, new ObjectMapper());
        Map<String, Object> message = Map.of("status", "REQUESTED");

        subscriber.receiveMessage(message);

        verify(messagingTemplate).convertAndSend("/topic/rides", message);
    }

    @Test
    void targetedOffersOnlyReachTheDriverDestination() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RedisSubscriber subscriber = new RedisSubscriber(messagingTemplate, new ObjectMapper());
        Map<String, Object> message = Map.of("rideId", 101L, "driverId", 11L,
                "rideStatus", "OFFER_SENT");

        Instant before = Instant.now();
        subscriber.receiveMessage(message);
        Instant after = Instant.now();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> payload = ArgumentCaptor.forClass(Map.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/drivers/11"), payload.capture());
        assertThat(Instant.parse((String) payload.getValue().get("serverTime")))
                .isBetween(before, after);
        assertThat(payload.getValue()).containsEntry("rideStatus", "OFFER_SENT");
        verify(messagingTemplate, never()).convertAndSend(eq("/topic/rides/101"), any(Object.class));
    }
}
