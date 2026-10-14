package com.swiftapp_backend.noticationservice.notification.infrastructure.messaging.kafka;

import com.swiftapp_backend.noticationservice.notification.application.service.NotificationInboxService;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;
import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

class ChatMessageNotificationListenerTest {
    @Test
    void forwardsChatAliasesToTheDurableDataOnlyInboxPath() {
        NotificationInboxService inbox = mock(NotificationInboxService.class);
        ChatMessageNotificationListener listener = new ChatMessageNotificationListener(inbox);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        Map<String, Object> event = Map.of(
                "messageId", "message-1",
                "sessionId", 8L,
                "rideId", 42L,
                "riderId", 7L,
                "driverId", 9L,
                "recipientId", 9L,
                "recipientRole", "DRIVER",
                "messageContent", "I am outside");

        listener.onMessage(record(event), acknowledgment);
        listener.onMessage(record(event), acknowledgment);

        verify(inbox, times(2)).deliverDataOnly(eq("CHAT_MESSAGE:message-1"), eq(42L),
                eq("CHAT_MESSAGE"), eq(9L), eq(UserRole.DRIVER),
                eq("New ride message"), eq("I am outside"), any(LocalDateTime.class),
                eq(Map.of(
                        "type", "CHAT_MESSAGE",
                        "messageId", "message-1",
                        "sessionId", "8",
                        "rideId", "42",
                        "riderId", "7",
                        "driverId", "9",
                        "messageContent", "I am outside",
                        "title", "New ride message",
                        "body", "I am outside")), eq(300L), eq(true));
        verify(acknowledgment, times(2)).acknowledge();
    }

    private ConsumerRecord<String, Map<String, Object>> record(Map<String, Object> event) {
        return new ConsumerRecord<>("chat.message.created", 0, 0L, "message-1", event);
    }
}
