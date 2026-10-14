package com.swiftapp_backend.noticationservice.notification.application.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.swiftapp_backend.noticationservice.notification.application.port.ProfileContactPort;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.notification.domain.model.UserFcmToken;
import com.swiftapp_backend.noticationservice.notification.domain.repository.UserFcmRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PushNotificationServiceTest {
    @SuppressWarnings("unchecked")
    private final ObjectProvider<FirebaseMessaging> provider = mock(ObjectProvider.class);
    private final FirebaseMessaging messaging = mock(FirebaseMessaging.class);
    private final UserFcmRepository tokens = mock(UserFcmRepository.class);
    private final ProfileContactPort contacts = mock(ProfileContactPort.class);
    private final PushNotificationService service = new PushNotificationService(provider, tokens, contacts);

    @Test
    void doesNotSendRideOfferWhenProfilePushPreferenceIsOff() throws Exception {
        when(provider.getIfAvailable()).thenReturn(messaging);
        when(tokens.findByUserIdAndRoleAndActiveTrueAndNotificationEnabledTrue(23L, UserRole.DRIVER))
                .thenReturn(List.of(device()));
        when(contacts.find(23L, UserRole.DRIVER)).thenReturn(
                new ProfileContactPort.Contact(23L, 8L, "Driver", "driver@example.com",
                        "DRIVER", false, true));

        PushDeliveryResult result = service.send(23L, UserRole.DRIVER,
                "New ride request", "Open the app",
                Map.of("type", "RIDE_OFFER", "rideId", "17"), 15);

        assertThat(result).isEqualTo(PushDeliveryResult.PREFERENCES_DISABLED);
        verify(messaging, never()).send(any(Message.class));
    }

    @Test
    void sendsToEnabledProfileAndRegisteredDevice() throws Exception {
        when(provider.getIfAvailable()).thenReturn(messaging);
        when(tokens.findByUserIdAndRoleAndActiveTrueAndNotificationEnabledTrue(23L, UserRole.DRIVER))
                .thenReturn(List.of(device()));
        when(contacts.find(23L, UserRole.DRIVER)).thenReturn(
                new ProfileContactPort.Contact(23L, 8L, "Driver", "driver@example.com",
                        "DRIVER", true, true));

        PushDeliveryResult result = service.send(23L, UserRole.DRIVER,
                "New ride request", "Open the app",
                Map.of("type", "RIDE_OFFER", "rideId", "17"), 15);

        assertThat(result).isEqualTo(PushDeliveryResult.SENT);
        verify(messaging).send(any(Message.class));
    }

    @Test
    void reportsNoDevicesWithoutLookingUpContactOrClaimingDelivery() throws Exception {
        when(provider.getIfAvailable()).thenReturn(messaging);
        when(tokens.findByUserIdAndRoleAndActiveTrueAndNotificationEnabledTrue(23L, UserRole.DRIVER))
                .thenReturn(List.of());

        PushDeliveryResult result = service.send(23L, UserRole.DRIVER,
                "New ride request", "Open the app", Map.of("rideId", "17"), 15);

        assertThat(result).isEqualTo(PushDeliveryResult.NO_DEVICES);
        verifyNoInteractions(contacts);
        verify(messaging, never()).send(any(Message.class));
    }

    @Test
    void skipsLegacyDeviceWithoutAuthIdInsteadOfCrashing() throws Exception {
        UserFcmToken incomplete = device();
        incomplete.setAuthUserId(null);
        when(provider.getIfAvailable()).thenReturn(messaging);
        when(tokens.findByUserIdAndRoleAndActiveTrueAndNotificationEnabledTrue(23L, UserRole.DRIVER))
                .thenReturn(List.of(incomplete));
        when(contacts.find(23L, UserRole.DRIVER)).thenReturn(
                new ProfileContactPort.Contact(23L, 8L, "Driver", "driver@example.com",
                        "DRIVER", true, true));

        PushDeliveryResult result = service.send(23L, UserRole.DRIVER,
                "New ride request", "Open the app", Map.of("rideId", "17"), 15);

        assertThat(result).isEqualTo(PushDeliveryResult.NO_VALID_DEVICES);
        verify(messaging, never()).send(any(Message.class));
    }

    @Test
    void reportsPushDisabledWhenFirebaseIsUnavailable() {
        when(provider.getIfAvailable()).thenReturn(null);

        PushDeliveryResult result = service.send(23L, UserRole.DRIVER,
                "New ride request", "Open the app", Map.of("rideId", "17"), 15);

        assertThat(result).isEqualTo(PushDeliveryResult.PUSH_DISABLED);
        verifyNoInteractions(tokens, contacts);
    }

    @Test
    void rideOptOutDoesNotSuppressAccountReviewAlert() throws Exception {
        when(provider.getIfAvailable()).thenReturn(messaging);
        when(tokens.findByUserIdAndRoleAndActiveTrueAndNotificationEnabledTrue(23L, UserRole.DRIVER))
                .thenReturn(List.of(device()));
        when(contacts.find(23L, UserRole.DRIVER)).thenReturn(
                new ProfileContactPort.Contact(23L, 8L, "Driver", "driver@example.com",
                        "DRIVER", true, true, false));

        assertThat(service.send(23L, UserRole.DRIVER, "New ride request", "Open Swift",
                Map.of("type", "RIDE_OFFER", "rideId", "17"), 15))
                .isEqualTo(PushDeliveryResult.PREFERENCES_DISABLED);
        assertThat(service.send(23L, UserRole.DRIVER, "Document approved", "Open Documents",
                Map.of("type", "DOCUMENT_APPROVED", "eventId", "review-17"), 300))
                .isEqualTo(PushDeliveryResult.SENT);
        verify(messaging, times(1)).send(any(Message.class));
    }

    @Test
    void chatUsesAppleAlertAndAndroidDataOnlyWithAuthenticatedRecipient() throws Exception {
        when(provider.getIfAvailable()).thenReturn(messaging);
        when(tokens.findByUserIdAndRoleAndActiveTrueAndNotificationEnabledTrue(23L, UserRole.DRIVER))
                .thenReturn(List.of(device()));
        when(contacts.find(23L, UserRole.DRIVER)).thenReturn(
                new ProfileContactPort.Contact(23L, 8L, "Driver", "driver@example.com",
                        "DRIVER", true, true));

        assertThat(service.sendDataOnly(23L, UserRole.DRIVER, "New ride message", "I'm at pickup",
                Map.of("type", "CHAT_MESSAGE", "rideId", "17", "sessionId", "25"), 300))
                .isEqualTo(PushDeliveryResult.SENT);

        ArgumentCaptor<Message> payload = ArgumentCaptor.forClass(Message.class);
        verify(messaging).send(payload.capture());
        JsonObject message = JsonParser.parseString(
                GsonFactory.getDefaultInstance().toString(payload.getValue())).getAsJsonObject();
        assertThat(message.has("notification")).isFalse();
        assertThat(message.getAsJsonObject("android").has("notification")).isFalse();
        assertThat(message.getAsJsonObject("data").get("authUserId").getAsString()).isEqualTo("8");
        JsonObject apns = message.getAsJsonObject("apns");
        assertThat(apns.getAsJsonObject("headers").get("apns-push-type").getAsString()).isEqualTo("alert");
        assertThat(apns.getAsJsonObject("headers").get("apns-priority").getAsString()).isEqualTo("10");
        JsonObject aps = apns.getAsJsonObject("payload").getAsJsonObject("aps");
        assertThat(aps.getAsJsonObject("alert").get("body").getAsString()).isEqualTo("I'm at pickup");
        assertThat(aps.get("category").getAsString()).isEqualTo("swift_chat");
        assertThat(aps.get("thread-id").getAsString()).isEqualTo("chat:25");
        assertThat(aps.has("content-available")).isFalse();
    }

    private static UserFcmToken device() {
        UserFcmToken token = new UserFcmToken();
        token.setId(1L);
        token.setUserId(23L);
        token.setAuthUserId(8L);
        token.setFcmToken("test-device-token");
        return token;
    }
}
