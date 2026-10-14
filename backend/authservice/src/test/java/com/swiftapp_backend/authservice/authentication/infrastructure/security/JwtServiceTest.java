package com.swiftapp_backend.authservice.authentication.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private static final String SECRET = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    private JwtService service;

    @BeforeEach
    void setUp() {
        service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", SECRET);
        ReflectionTestUtils.setField(service, "jwtExpiration", 60000L);
        ReflectionTestUtils.setField(service, "refreshExpiration", 120000L);
    }

    @Test
    void accessAndRefreshTokensCarryPersistedUserId() {
        for (String token : List.of(
                service.generateAccessToken("driver@example.com", 42L),
                service.generateRefreshToken("driver@example.com", 42L))) {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Decoders.BASE64.decode(SECRET))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            assertEquals("driver@example.com", claims.getSubject());
            assertEquals(42L, ((Number) claims.get("userId")).longValue());
        }
    }

    @Test
    void refusesToIssueTokenWithoutPersistedUserId() {
        assertThrows(IllegalArgumentException.class,
                () -> service.generateAccessToken("driver@example.com", null));
    }
}
