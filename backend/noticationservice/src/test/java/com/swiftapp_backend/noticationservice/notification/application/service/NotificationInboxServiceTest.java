package com.swiftapp_backend.noticationservice.notification.application.service;

import com.swiftapp_backend.noticationservice.notification.application.port.RecipientProfilePort;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.notification.domain.repository.InboxNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationInboxServiceTest {
    @Autowired private NotificationInboxService service;
    @Autowired private InboxNotificationRepository repository;
    @MockitoBean private RecipientProfilePort recipientProfiles;
    @MockitoBean private PushNotificationService push;

    private final LocalDateTime occurredAt = LocalDateTime.of(2026, 10, 10, 10, 30);
    private final Map<String, String> data = Map.of("rideId", "101", "type", "COMPLETED");

    @BeforeEach
    void clear() {
        repository.deleteAll();
        reset(push, recipientProfiles);
    }

    @Test
    void persistsOnceAcrossAliasDeliveryAndEnforcesInboxOwnership() {
        when(push.send(42L, UserRole.RIDER, "Ride completed", "Open the receipt", data, 300))
                .thenReturn(PushDeliveryResult.SENT);
        service.deliver("COMPLETED:101", 101L, "COMPLETED", 42L, UserRole.RIDER,
                "Ride completed", "Open the receipt", occurredAt, data, 300, true);
        service.deliver("COMPLETED:101", 101L, "COMPLETED", 42L, UserRole.RIDER,
                "Ride completed", "Open the receipt", occurredAt, data, 300, true);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findByEventKeyAndRecipientIdAndRecipientRole(
                "COMPLETED:101", 42L, UserRole.RIDER).orElseThrow().isPushDelivered()).isTrue();
        verify(push, times(1)).send(42L, UserRole.RIDER, "Ride completed",
                "Open the receipt", data, 300);
        when(recipientProfiles.profileId(7L, UserRole.RIDER)).thenReturn(42L);
        when(recipientProfiles.profileId(8L, UserRole.RIDER)).thenReturn(43L);

        var rows = service.list(7L, UserRole.RIDER, 50);
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().read()).isFalse();
        assertThat(rows.getFirst().createdAt()).isEqualTo(occurredAt);
        assertThat(service.unreadCount(7L, UserRole.RIDER)).isEqualTo(1);
        assertThatThrownBy(() -> service.markRead(8L, UserRole.RIDER, rows.getFirst().id()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        assertThat(service.markRead(7L, UserRole.RIDER, rows.getFirst().id()).read()).isTrue();
        assertThat(service.unreadCount(7L, UserRole.RIDER)).isZero();
    }

    @Test
    void failedDriverPushRetriesWithoutDuplicatingSuccessfulRiderPush() {
        when(push.send(42L, UserRole.RIDER, "Ride completed", "Open the receipt", data, 300))
                .thenReturn(PushDeliveryResult.SENT);
        when(push.send(eq(11L), eq(UserRole.DRIVER), anyString(), anyString(), eq(data), eq(300L)))
                .thenThrow(new IllegalStateException("temporary FCM error"))
                .thenReturn(PushDeliveryResult.SENT);

        service.deliver("COMPLETED:101", 101L, "COMPLETED", 42L, UserRole.RIDER,
                "Ride completed", "Open the receipt", occurredAt, data, 300, true);
        assertThatThrownBy(() -> service.deliver("COMPLETED:101", 101L, "COMPLETED",
                11L, UserRole.DRIVER, "Ride completed", "Trip ended", occurredAt,
                data, 300, true)).isInstanceOf(IllegalStateException.class);
        assertThat(repository.count()).isEqualTo(1);

        service.deliver("COMPLETED:101", 101L, "COMPLETED", 42L, UserRole.RIDER,
                "Ride completed", "Open the receipt", occurredAt, data, 300, true);
        service.deliver("COMPLETED:101", 101L, "COMPLETED", 11L, UserRole.DRIVER,
                "Ride completed", "Trip ended", occurredAt, data, 300, true);

        assertThat(repository.count()).isEqualTo(2);
        verify(push, times(1)).send(42L, UserRole.RIDER, "Ride completed",
                "Open the receipt", data, 300);
        verify(push, times(2)).send(11L, UserRole.DRIVER, "Ride completed",
                "Trip ended", data, 300);
    }

    @Test
    void chatInboxKeepsSessionAndMessageIdsForNavigation() {
        Map<String, String> chatData = Map.of("type", "CHAT_MESSAGE", "rideId", "101",
                "sessionId", "51", "messageId", "9f8e");
        when(push.sendDataOnly(42L, UserRole.RIDER, "New ride message", "Hello", chatData, 300))
                .thenReturn(PushDeliveryResult.SENT);
        service.deliverDataOnly("CHAT_MESSAGE:9f8e", 101L, "CHAT_MESSAGE",
                42L, UserRole.RIDER, "New ride message", "Hello", occurredAt,
                chatData, 300, true);
        when(recipientProfiles.profileId(7L, UserRole.RIDER)).thenReturn(42L);

        var notification = service.list(7L, UserRole.RIDER, 50).getFirst();
        assertThat(notification.sessionId()).isEqualTo(51L);
        assertThat(notification.messageId()).isEqualTo("9f8e");
        verify(push).sendDataOnly(42L, UserRole.RIDER, "New ride message",
                "Hello", chatData, 300);
    }

    @Test
    void noDeviceKeepsInboxUnreadAndUndeliveredWithoutRetryingTheKafkaRecord() {
        when(push.send(42L, UserRole.RIDER, "Ride completed", "Open the receipt", data, 300))
                .thenReturn(PushDeliveryResult.NO_DEVICES);

        service.deliver("COMPLETED:101", 101L, "COMPLETED", 42L, UserRole.RIDER,
                "Ride completed", "Open the receipt", occurredAt, data, 300, true);

        var saved = repository.findByEventKeyAndRecipientIdAndRecipientRole(
                "COMPLETED:101", 42L, UserRole.RIDER).orElseThrow();
        assertThat(saved.isPushDelivered()).isFalse();
        assertThat(saved.getReadAt()).isNull();
        verify(push).send(42L, UserRole.RIDER, "Ride completed", "Open the receipt", data, 300);
    }

    @Test
    void staleHistoryIsStoredButNeverPushedEvenWhenLaterAliased() {
        service.deliver("COMPLETED:101", 101L, "COMPLETED", 42L, UserRole.RIDER,
                "Ride completed", "Open the receipt", occurredAt, data, 300, false);
        service.deliver("COMPLETED:101", 101L, "COMPLETED", 42L, UserRole.RIDER,
                "Ride completed", "Open the receipt", occurredAt, data, 300, false);

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findByEventKeyAndRecipientIdAndRecipientRole(
                "COMPLETED:101", 42L, UserRole.RIDER).orElseThrow().isPushDelivered()).isFalse();
        verifyNoInteractions(push);
    }
}
