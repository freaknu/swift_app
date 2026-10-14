package com.swiftapp_backend.authservice.authentication.infrastructure.persistence;

import com.swiftapp_backend.authservice.authentication.infrastructure.persistence.entity.UserCredentialsEntity;
import com.swiftapp_backend.authservice.authentication.infrastructure.persistence.repository.SpringDataUserCredentialRepository;
import com.swiftapp_backend.authservice.authentication.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth_token_persistence;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.hbm2ddl.halt_on_error=true"
})
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserCredentialsTokenPersistenceTest {
    @Autowired private SpringDataUserCredentialRepository repository;
    @Autowired private TestEntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void longEmailSignedJwtSessionsRoundTripWithoutTruncationInActualJpaSchema() {
        JwtService tokens = new JwtService();
        ReflectionTestUtils.setField(tokens, "secretKey", Base64.getEncoder().encodeToString(
                "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8)));
        ReflectionTestUtils.setField(tokens, "jwtExpiration", 60000L);
        ReflectionTestUtils.setField(tokens, "refreshExpiration", 120000L);
        String email = "rider." + "x".repeat(45) + "@" + "a".repeat(45) + ".example.test";
        String access = tokens.generateAccessToken(email, 7L);
        String refresh = tokens.generateRefreshToken(email, 7L);
        assertTrue(access.length() > 255);
        assertTrue(refresh.length() > 255);

        for (String column : new String[]{"access_token", "refresh_token"}) {
            Long schemaCapacity = jdbc.queryForObject("""
                    SELECT character_maximum_length FROM information_schema.columns
                    WHERE LOWER(table_name) = 'user_credentials' AND LOWER(column_name) = ?
                    """, Long.class, column);
            assertNotNull(schemaCapacity);
            assertTrue(schemaCapacity > 255, "JWT column must not retain the old VARCHAR(255) limit");
        }

        Long id = repository.saveAndFlush(UserCredentialsEntity.builder()
                .userId(7L).accessToken(access).refreshToken(refresh).deviceName("Test device").build()).getId();
        entityManager.clear();
        UserCredentialsEntity reloaded = repository.findById(id).orElseThrow();
        assertEquals(access, reloaded.getAccessToken());
        assertEquals(refresh, reloaded.getRefreshToken());
        assertEquals(id, repository.findByAccessToken(access).orElseThrow().getId());
        assertEquals(id, repository.findByRefreshToken(refresh).orElseThrow().getId());
        assertTrue(tokens.validateToken(reloaded.getAccessToken()));
        assertTrue(tokens.validateToken(reloaded.getRefreshToken()));
        assertEquals(email, tokens.extractUsername(reloaded.getAccessToken()));
    }
}
