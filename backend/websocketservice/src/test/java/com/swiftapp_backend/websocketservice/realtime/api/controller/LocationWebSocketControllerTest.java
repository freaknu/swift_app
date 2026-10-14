package com.swiftapp_backend.websocketservice.realtime.api.controller;

import com.swiftapp_backend.websocketservice.realtime.api.dto.LocationUpdateDto;
import com.swiftapp_backend.websocketservice.realtime.application.service.LiveLocationService;
import com.swiftapp_backend.websocketservice.realtime.infrastructure.config.DriverIdentityClient;
import com.swiftapp_backend.websocketservice.realtime.infrastructure.config.DriverSubscriptionInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LocationWebSocketControllerTest {
    private final LiveLocationService locations = mock(LiveLocationService.class);
    private final DriverIdentityClient identities = mock(DriverIdentityClient.class);
    private final LocationWebSocketController controller =
            new LocationWebSocketController(locations, identities);

    @Test
    void onlyVerifiedConnectIdentityCanPublishDriverLocation() {
        LocationUpdateDto location = LocationUpdateDto.builder()
                .driverId(7L).rideId(19L).latitude(12.9).longitude(77.6).build();
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionAttributes(Map.of(DriverSubscriptionInterceptor.AUTH_USER_ID_ATTRIBUTE, 42L));
        headers.setNativeHeader("x-user-id", "999");
        when(identities.belongsTo(42L, 7L)).thenReturn(true);

        controller.consumeLocation(headers, location);

        verify(identities).belongsTo(42L, 7L);
        verify(locations).consumeLocation(any());
        verify(identities, never()).belongsTo(999L, 7L);
    }

    @Test
    void nativeHeaderCannotReplaceMissingConnectIdentity() {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setNativeHeader("x-user-id", "42");
        assertThrows(MessagingException.class, () -> controller.consumeLocation(headers,
                LocationUpdateDto.builder().driverId(7L).build()));
        verifyNoInteractions(locations, identities);
    }
}
