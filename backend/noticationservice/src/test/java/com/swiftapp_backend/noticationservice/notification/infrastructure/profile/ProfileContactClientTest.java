package com.swiftapp_backend.noticationservice.notification.infrastructure.profile;

import com.sun.net.httpserver.HttpServer;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileContactClientTest {
    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    @Test
    void readsCanonicalProfileIdContactAndNotificationPreferences() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/profiles/users/31", exchange -> {
            byte[] body = """
                    {"success":true,"data":{"id":31,"authUserId":7,"fullName":"Rider",
                     "email":"rider@example.com","role":"RIDER",
                     "pushNotificationEnabled":false,"emailNotificationEnabled":true}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.createContext("/internal/profiles/drivers/23", exchange -> {
            byte[] body = """
                    {"success":true,"data":{"id":23,"authUserId":8,"fullName":"Driver",
                     "email":"driver@example.com","role":"DRIVER",
                     "pushNotificationEnabled":true,"emailNotificationEnabled":true,
                     "rideAlertsEnabled":false}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        ProfileContactClient client = new ProfileContactClient(base, base);

        var contact = client.find(31L, UserRole.RIDER);

        assertThat(contact.id()).isEqualTo(31L);
        assertThat(contact.authUserId()).isEqualTo(7L);
        assertThat(contact.email()).isEqualTo("rider@example.com");
        assertThat(contact.pushNotificationEnabled()).isFalse();
        assertThat(contact.emailNotificationEnabled()).isTrue();
        assertThat(contact.rideAlertsEnabled()).isNull();
        assertThat(client.find(32L, UserRole.RIDER)).isNull();

        var driver = client.find(23L, UserRole.DRIVER);
        assertThat(driver.pushNotificationEnabled()).isTrue();
        assertThat(driver.rideAlertsEnabled()).isFalse();
    }
}
