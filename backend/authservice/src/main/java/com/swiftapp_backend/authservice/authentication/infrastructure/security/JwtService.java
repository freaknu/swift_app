package com.swiftapp_backend.authservice.authentication.infrastructure.security;

import com.swiftapp_backend.authservice.authentication.application.port.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService implements TokenProvider {

    @Value("${secret_key}")
    private String secretKey;

    @Value("${jwt_expiration}")
    private Long jwtExpiration;

    @Value("${refresh_expiration}")
    private Long refreshExpiration;

    private Key getKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    @Override
    public String generateAccessToken(String username, Long userId) {
        return generateToken(username, userId, jwtExpiration);
    }

    @Override
    public String generateRefreshToken(String username, Long userId) {
        return generateToken(username, userId, refreshExpiration);
    }

    private String generateToken(String username, Long userId, Long expiration) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("A persisted user ID is required for JWT generation");
        }
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(Date.from(Instant.now()))
                .setExpiration(
                        Date.from(
                                Instant.now().plusMillis(expiration)
                        )
                )
                .signWith(getKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(
            String token,
            UserDetails userDetails
    ) {
        String username = extractUsername(token);

        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration)
                .before(new Date());
    }

    private <T> T extractClaim(
            String token,
            Function<Claims, T> claimsResolver
    ) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
