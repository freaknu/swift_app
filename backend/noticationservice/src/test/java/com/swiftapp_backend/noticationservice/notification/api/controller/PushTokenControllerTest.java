package com.swiftapp_backend.noticationservice.notification.api.controller;

import com.swiftapp_backend.noticationservice.notification.application.service.PushDeliveryResult;
import com.swiftapp_backend.noticationservice.notification.application.service.PushNotificationService;
import com.swiftapp_backend.noticationservice.notification.application.service.PushTokenService;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.notification.domain.model.UserFcmToken;
import com.swiftapp_backend.noticationservice.shared.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PushTokenControllerTest {
    private static final String INSTALLATION = "f36a9ad7-872e-4f84-8838-4ed28ba98025";
    private final PushTokenService devices = mock(PushTokenService.class);
    private final PushNotificationService push = mock(PushNotificationService.class);
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders.standaloneSetup(new PushTokenController(devices, push))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void statusReturnsPermissionStateWithoutToken() throws Exception {
        when(devices.ownedDevice(500L, INSTALLATION)).thenReturn(device(true));
        mvc.perform(get("/api/v1/notifications/devices/" + INSTALLATION).header("x-user-id", "500"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.registered").value(true))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(content().string(not(containsString("private-device-token"))));
    }

    @Test
    void testPushRequiresOwnedDevice() throws Exception {
        when(devices.ownedDevice(501L, INSTALLATION)).thenThrow(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        mvc.perform(post("/api/v1/notifications/devices/" + INSTALLATION + "/test")
                        .header("x-user-id", "501")).andExpect(status().isNotFound());
        verifyNoInteractions(push);
    }

    @Test
    void testPushReportsProviderAcceptance() throws Exception {
        when(devices.ownedDevice(500L, INSTALLATION)).thenReturn(device(true));
        when(push.send(eq(23L), eq(UserRole.DRIVER), anyString(), anyString(), anyMap(), eq(60L)))
                .thenReturn(PushDeliveryResult.SENT);
        mvc.perform(post("/api/v1/notifications/devices/" + INSTALLATION + "/test")
                        .header("x-user-id", "500")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deliveryStatus").value("SENT"));
    }

    @Test
    void deniedPermissionDoesNotAttemptSend() throws Exception {
        when(devices.ownedDevice(500L, INSTALLATION)).thenReturn(device(false));
        mvc.perform(post("/api/v1/notifications/devices/" + INSTALLATION + "/test")
                        .header("x-user-id", "500")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deliveryStatus").value("PREFERENCES_DISABLED"));
        verifyNoInteractions(push);
    }

    private UserFcmToken device(boolean enabled) {
        UserFcmToken device = new UserFcmToken();
        device.setAuthUserId(500L);
        device.setUserId(23L);
        device.setRole(UserRole.DRIVER);
        device.setActive(true);
        device.setNotificationEnabled(enabled);
        device.setFcmToken("private-device-token");
        return device;
    }
}
