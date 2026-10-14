package com.swiftapp_backend.authservice.authentication.application.port;

public interface TokenProvider {
    String generateAccessToken(String username, Long userId);

    String generateRefreshToken(String username, Long userId);

    boolean validateToken(String token);

    String extractUsername(String token);
}
