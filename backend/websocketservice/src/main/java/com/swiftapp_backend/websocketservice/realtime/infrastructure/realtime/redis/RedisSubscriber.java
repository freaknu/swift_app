package com.swiftapp_backend.websocketservice.realtime.infrastructure.realtime.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RedisSubscriber {
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public void receiveMessage(Object message) {
        Map<String, Object> payload = toPayload(message);
        Long rideId = extractLong(payload, "rideId");
        Long driverId = extractLong(payload, "driverId");
        Long riderId = extractLong(payload, "riderId");
        String rideStatus = extractText(payload, "rideStatus");
        String messageId = extractText(payload, "messageId");
        Long sessionId = extractLong(payload, "sessionId");

        Object outgoing = message;
        if ("OFFER_SENT".equals(rideStatus)) {
            // Stamp at the STOMP boundary, after Kafka and Redis transit. A
            // delayed message must still report the current server clock.
            Map<String, Object> stamped = new LinkedHashMap<>(payload);
            stamped.put("serverTime", Instant.now().toString());
            outgoing = stamped;
        }

        for (String destination : resolveDestinations(rideId, riderId, driverId, rideStatus, messageId, sessionId)) {
            messagingTemplate.convertAndSend(destination, outgoing);
        }
    }

    private Map<String, Object> toPayload(Object message) {
        if (message == null) {
            return Map.of();
        }

        try {
            return objectMapper.convertValue(
                    message,
                    new TypeReference<>() {}
            );
        } catch (IllegalArgumentException ignored) {
            return Map.of();
        }
    }

    private Long extractLong(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.valueOf(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String extractText(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }

    private Set<String> resolveDestinations(Long rideId, Long riderId, Long driverId, String rideStatus,
                                            String messageId, Long sessionId) {
        Set<String> destinations = new LinkedHashSet<>();
        if (messageId != null && sessionId != null) {
            destinations.add("/topic/chat/session/" + sessionId);
            return destinations;
        }
        if ("OFFER_SENT".equals(rideStatus) || "OFFER_WITHDRAWN".equals(rideStatus)) {
            if (driverId != null) destinations.add("/topic/drivers/" + driverId);
            return destinations;
        }
        if (rideId != null) {
            destinations.add("/topic/rides/" + rideId);
        }
        if (riderId != null) {
            destinations.add("/topic/riders/" + riderId);
        }
        if (driverId != null) {
            destinations.add("/topic/drivers/" + driverId);
        }
        if (destinations.isEmpty()) {
            destinations.add("/topic/rides");
        }
        return destinations;
    }
}
