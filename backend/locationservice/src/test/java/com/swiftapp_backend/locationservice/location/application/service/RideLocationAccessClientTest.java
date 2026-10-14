package com.swiftapp_backend.locationservice.location.application.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RideLocationAccessClientTest {
    private HttpServer server;
    private RideLocationAccessClient client;
    private final AtomicReference<String> status = new AtomicReference<>("DRIVER_ASSIGNED");

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/rides/7", exchange -> respond(exchange,
                "{\"success\":true,\"data\":{\"id\":7,\"riderId\":91,\"driverId\":55,"
                        + "\"currentStatus\":\"" + status.get() + "\",\"pickupLatitude\":12.97,"
                        + "\"pickupLongitude\":77.59}}"));
        server.createContext("/internal/profiles/drivers/55", exchange -> respond(exchange,
                "{\"success\":true,\"data\":{\"id\":55,\"authUserId\":42}}"));
        server.createContext("/internal/profiles/users/91", exchange -> respond(exchange,
                "{\"success\":true,\"data\":{\"id\":91,\"authUserId\":901}}"));
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        client = new RideLocationAccessClient(base, base, base);
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void acceptsAssignedDriverOnlyWhileRideIsActive() {
        assertEquals(91L, client.assignedToDriver(7L, 55L).riderId());
        assertEquals(12.97, client.assignedToDriver(7L, 55L).pickupLatitude());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> client.assignedToDriver(7L, 56L)).getStatusCode().value());
        status.set("CANCELLED");
        assertEquals(409, assertThrows(ResponseStatusException.class,
                () -> client.assignedToDriver(7L, 55L)).getStatusCode().value());
    }

    @Test
    void snapshotAccessIsLimitedToRideParticipants() {
        assertEquals(55L, client.participant(7L, 55L, 42L).driverId());
        assertEquals(91L, client.participant(7L, null, 901L).riderId());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> client.participant(7L, null, 999L)).getStatusCode().value());
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> client.participant(7L, 56L, 901L)).getStatusCode().value());
    }

    private void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var stream = exchange.getResponseBody()) {
            stream.write(bytes);
        }
    }
}
