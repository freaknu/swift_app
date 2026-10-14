package com.swiftapp_backend.authservice.authentication.infrastructure.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.net.ServerSocket;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Runs against an isolated ephemeral Redis, never the developer's configured database. */
class RedisOtpLifecycleTest {
    @TempDir static Path directory;
    private static Process server;
    private static LettuceConnectionFactory connectionFactory;
    private static RedisTemplate<String, Object> template;
    private static RedisService store;
    private String key;

    @BeforeAll
    static void startRedis() throws Exception {
        String executable = System.getenv().getOrDefault("REDIS_SERVER_BINARY", "redis-server");
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        try {
            server = new ProcessBuilder(executable, "--bind", "127.0.0.1", "--port", String.valueOf(port),
                    "--save", "", "--appendonly", "no", "--dir", directory.toString())
                    .redirectErrorStream(true).redirectOutput(directory.resolve("redis.log").toFile()).start();
        } catch (java.io.IOException unavailable) {
            Assumptions.abort("redis-server is unavailable; set REDIS_SERVER_BINARY to run Redis lifecycle tests");
        }
        connectionFactory = new LettuceConnectionFactory("127.0.0.1", port);
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        template = new RedisConfig().redisTemplate(connectionFactory);
        for (int attempt = 0; attempt < 100; attempt++) {
            try (var connection = connectionFactory.getConnection()) {
                if ("PONG".equals(connection.ping())) break;
            } catch (Exception waiting) { Thread.sleep(20); }
        }
        store = new RedisService(template, new ObjectMapper());
    }

    @AfterAll
    static void stopRedis() {
        if (connectionFactory != null) connectionFactory.destroy();
        if (server != null) server.destroy();
    }

    @BeforeEach void uniqueKey() { key = "otp-test:" + UUID.randomUUID(); }

    @Test void wrongVerificationDoesNotDeleteAndValidVerificationPreservesOriginalExpiry() {
        store.store(key, 123456, Duration.ofSeconds(60));
        long before = template.getExpire(key, TimeUnit.MILLISECONDS);
        assertFalse(store.verify(key, 999999));
        assertEquals(123456, store.find(key).orElseThrow());
        assertTrue(store.verify(key, 123456));
        assertEquals(123456, store.find(key).orElseThrow());
        assertTrue(template.getExpire(key, TimeUnit.MILLISECONDS) <= before);
        assertTrue(template.getExpire(key + ":verified", TimeUnit.MILLISECONDS) <= before);
    }

    @Test void claimRequiresVerifiedMatchingCodeAndCanIssueOnlyOneSession() {
        store.store(key, 123456, Duration.ofSeconds(60));
        assertFalse(store.claimVerified(key, 123456, "one", "session-one"));
        assertTrue(store.verify(key, 123456));
        assertFalse(store.claimVerified(key, 999999, "one", "session-one"));
        assertTrue(store.claimVerified(key, 123456, "one", "session-one"));
        assertFalse(store.claimVerified(key, 123456, "two", "session-two"));
        assertFalse(store.verify(key, 123456));
        assertEquals(123456, store.find(key).orElseThrow());
        assertFalse(store.consume(key, 123456));
    }

    @Test void onlyMatchingCompletionAndAccessSessionCanClearRetainedOtp() {
        store.store(key, 123456, Duration.ofSeconds(60));
        store.verify(key, 123456);
        assertTrue(store.claimVerified(key, null, "completion", "session"));
        store.complete(key, "wrong", "session");
        store.complete(key, "completion", "other-session");
        assertTrue(store.find(key).isPresent());
        store.complete(key, "completion", "session");
        assertTrue(store.find(key).isEmpty());
        store.complete(key, "completion", "session");
        assertFalse(template.hasKey(key + ":verified"));
        assertFalse(template.hasKey(key + ":completion"));
        assertFalse(template.hasKey(key + ":session"));
    }

    @Test void resendEvenWithSameCodeCannotBeClearedByOldAcknowledgementOrRollback() {
        store.store(key, 123456, Duration.ofSeconds(60));
        store.verify(key, 123456);
        store.claimVerified(key, 123456, "old", "old-session");
        store.store(key, 123456, Duration.ofSeconds(60));
        store.complete(key, "old", "old-session");
        assertEquals(123456, store.find(key).orElseThrow());
        assertFalse(store.claimVerified(key, 123456, "new", "new-session"));
        store.verify(key, 123456);
        assertTrue(store.claimVerified(key, 123456, "new", "new-session"));
        store.release(key, "old", "old-session");
        store.complete(key, "old", "old-session");
        assertTrue(store.find(key).isPresent());
        store.complete(key, "new", "new-session");
        assertTrue(store.find(key).isEmpty());
    }

    @Test void transactionRollbackAllowsRetryWithoutExtendingCodeExpiry() {
        store.store(key, 123456, Duration.ofSeconds(60));
        store.verify(key, 123456);
        store.claimVerified(key, 123456, "failed", "session");
        long remaining = template.getExpire(key, TimeUnit.MILLISECONDS);
        store.release(key, "failed", "session");
        assertTrue(store.claimVerified(key, 123456, "retry", "session"));
        assertTrue(template.getExpire(key, TimeUnit.MILLISECONDS) <= remaining);
    }

    @Test void expiryAndNonLoginConsumptionRetainOneTimeSemantics() throws Exception {
        store.store(key, 123456, Duration.ofMillis(150));
        store.verify(key, 123456);
        store.claimVerified(key, 123456, "completion", "session");
        Thread.sleep(200);
        assertTrue(store.find(key).isEmpty());
        assertFalse(store.verify(key, 123456));
        assertFalse(store.claimVerified(key, 123456, "retry", "session"));
        store.complete(key, "completion", "session");
        store.store(key, 234567, Duration.ofSeconds(60));
        assertFalse(store.consume(key, 123456));
        assertEquals(234567, store.find(key).orElseThrow());
        assertTrue(store.consume(key, 234567));
        assertFalse(store.consume(key, 234567));
    }
}
