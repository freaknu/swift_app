package com.swiftapp_backend.authservice.authentication.api.controller;

import com.swiftapp_backend.authservice.authentication.application.service.AuthService;
import com.swiftapp_backend.authservice.shared.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthOtpControllerTest {
    private final AuthService auth = mock(AuthService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AuthController(auth))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test void incorrectOrExpiredOtpIsAnActualFailureEnvelope() throws Exception {
        mvc.perform(get("/api/v1/auth/verifyOtp/rider@example.test/999999"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.data").value(false))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired OTP"));
    }

    @Test void correctOtpReturnsBooleanTrue() throws Exception {
        when(auth.isCorrectOtp("rider@example.test", 123456)).thenReturn(true);
        mvc.perform(get("/api/v1/auth/verifyOtp/rider@example.test/123456"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test void completionRejectsMissingBearerAndPassesOnlyAccessTokenAndOpaqueClaim() throws Exception {
        mvc.perform(post("/api/v1/auth/home-ready").contentType("application/json")
                .content("{\"otpCompletionToken\":\"completion\"}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.success").value(false));
        verifyNoInteractions(auth);
        mvc.perform(post("/api/v1/auth/home-ready").header("Authorization", "Bearer access")
                .header("X-User-Id", "999").contentType("application/json")
                .content("{\"otpCompletionToken\":\"completion\",\"email\":\"other@example.test\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        verify(auth).acknowledgeHomeReady("access", "completion");
    }
}
