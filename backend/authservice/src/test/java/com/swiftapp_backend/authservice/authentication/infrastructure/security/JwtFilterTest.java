package com.swiftapp_backend.authservice.authentication.infrastructure.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class JwtFilterTest {
    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void malformedAndExpiredBearerTokensReturnUnauthorizedInsteadOfServerError() throws Exception {
        for (RuntimeException failure : new RuntimeException[]{new MalformedJwtException("malformed"),
                new ExpiredJwtException(null, null, "expired")}) {
            JwtService jwt = mock(JwtService.class);
            when(jwt.extractUsername("invalid")).thenThrow(failure);
            CustomUserDetailsService users = mock(CustomUserDetailsService.class);
            JwtFilter filter = new JwtFilter(jwt, users);
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/home-ready");
            request.addHeader("Authorization", "Bearer invalid");
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            filter.doFilter(request, response, chain);
            assertEquals(401, response.getStatus());
            verifyNoInteractions(chain, users);
        }
    }
}
