package com.swiftapp_backend.websocketservice.realtime.infrastructure.config;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class RideSubscriptionAccessClientTest {
    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void authorizesRideTopicFromInternalPersistedRideParticipants() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/rides/17", exchange -> {
            byte[] body = """
                    {"success":true,"data":{"id":17,"riderId":31,"driverId":23,
                     "currentStatus":"DRIVER_ASSIGNED"}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        RiderIdentityClient riders = mock(RiderIdentityClient.class);
        DriverIdentityClient drivers = mock(DriverIdentityClient.class);
        when(riders.belongsTo(7L, 31L)).thenReturn(true);
        RideSubscriptionAccessClient client = new RideSubscriptionAccessClient(
                "http://127.0.0.1:" + server.getAddress().getPort(), riders, drivers);

        assertThat(client.belongsTo(7L, 17L)).isTrue();
        assertThat(client.belongsTo(8L, 17L)).isFalse();
        assertThat(client.belongsTo(7L, 18L)).isFalse();
        verify(riders).belongsTo(7L, 31L);
        verify(drivers).belongsTo(8L, 23L);
    }
}
