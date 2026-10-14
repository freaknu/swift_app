package com.swiftapp_backend.authservice.authentication.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.authservice.authentication.application.service.UserCredentialService;
import com.swiftapp_backend.authservice.authentication.domain.model.UserCredentials;
import com.swiftapp_backend.authservice.authentication.domain.repository.UserCredentialRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authorization.AuthorizationDeniedException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UserCredentialControllerTest {
    @Test
    void credentialListOnlyReadsAuthenticatedAccountsOwnSessions() {
        UserCredentialService service = mock(UserCredentialService.class);
        UserCredentialController controller = new UserCredentialController(service);

        assertThrows(AuthorizationDeniedException.class,
                () -> controller.getAllCredentials(7L, 8L));
        verifyNoInteractions(service);
        when(service.getAllCredentials(7L)).thenReturn(List.of());
        assertEquals(0, controller.getAllCredentials(7L, 7L).getBody().getData().size());
    }

    @Test
    void sessionResponseNeverSerializesTokensPinOrNetworkIdentity() throws Exception {
        UserCredentialRepository repository = mock(UserCredentialRepository.class);
        UserCredentialService service = new UserCredentialService(repository);
        when(repository.findAllByUserId(7L)).thenReturn(List.of(UserCredentials.builder()
                .id(1L).userId(7L).accessToken("sensitive-access")
                .refreshToken("sensitive-refresh").appLockPin(1234)
                .ipAddress("192.0.2.1").fingerPrint("sensitive-device-id")
                .deviceName("My phone").os("Android").build()));

        String response = new ObjectMapper().writeValueAsString(service.getAllCredentials(7L));
        assertFalse(response.contains("sensitive-access"));
        assertFalse(response.contains("sensitive-refresh"));
        assertFalse(response.contains("1234"));
        assertFalse(response.contains("192.0.2.1"));
        assertFalse(response.contains("sensitive-device-id"));
        assertFalse(response.contains("Token"));
        assertFalse(response.contains("appLockPin"));
    }
}
