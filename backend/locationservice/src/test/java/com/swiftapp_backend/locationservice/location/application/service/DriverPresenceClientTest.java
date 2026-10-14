package com.swiftapp_backend.locationservice.location.application.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DriverPresenceClientTest {
    private HttpServer server;
    private DriverPresenceClient client;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/driver/profile", exchange -> {
            boolean owner = "42".equals(exchange.getRequestHeaders().getFirst("x-user-id"));
            String body = owner ? """
                    {"success":true,"data":{"driverId":55,"driverStatus":"ONLINE",
                    "active":true,"allVehicles":[{"vehicleType":"SEDAN","active":true}]}}
                    """ : "{" + "\"success\":true,\"data\":{\"driverId\":56}}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.start();
        client = new DriverPresenceClient("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stop() { server.stop(0); }

    @Test
    void onlineOwnedProfileSuppliesCanonicalVehicleType() {
        var presence = client.get(42L, 55L);
        assertEquals("ACTIVE", presence.driverStatus());
        assertEquals("SEDAN", presence.vehicleTypeEnum());
    }

    @Test
    void rejectsMismatchedDriverProfileId() {
        assertEquals(403, assertThrows(ResponseStatusException.class,
                () -> client.get(999L, 55L)).getStatusCode().value());
    }
}
