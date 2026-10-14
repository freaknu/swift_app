package com.swiftapp_backend.chatservice.chat.infrastructure.client;

import com.swiftapp_backend.chatservice.chat.application.port.ChatIdentityPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Chat stores rider/driver profile ids, while the gateway forwards the auth
 * account id. Both profile services are queried so a user that has more than
 * one profile can still be authorized against the session participant.
 */
@Slf4j
@Component
public class ChatIdentityHttpClient implements ChatIdentityPort {
    private final RestClient userClient;
    private final RestClient driverClient;

    public ChatIdentityHttpClient(
            @Value("${services.user-url:http://localhost:8082}") String userUrl,
            @Value("${services.driver-url:http://localhost:8096}") String driverUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.userClient = RestClient.builder().baseUrl(userUrl).requestFactory(factory).build();
        this.driverClient = RestClient.builder().baseUrl(driverUrl).requestFactory(factory).build();
    }

    @Override
    public Identity identityForAuthUser(Long authUserId) {
        if (authUserId == null || authUserId <= 0) {
            return new Identity(authUserId, null, null);
        }
        Long riderId = readId(userClient, "/api/v1/user/getUserDetails", "user", "id", authUserId)
                .orElse(null);
        Long driverId = readId(driverClient, "/api/v1/driver/driverMiniDetail", "driver", "driverId", authUserId)
                .orElse(null);
        return new Identity(authUserId, riderId, driverId);
    }

    private java.util.Optional<Long> readId(RestClient client, String uri, String dataKey,
                                            String idKey, Long authUserId) {
        try {
            Map<?, ?> response = client.get().uri(uri)
                    .header("x-user-id", authUserId.toString())
                    .retrieve().body(Map.class);
            if (response == null || !(response.get("data") instanceof Map<?, ?> data)) {
                return java.util.Optional.empty();
            }
            // Current profile endpoints return the DTO directly under data;
            // accept the older nested {data:{user/driver:{...}}} form too.
            Map<?, ?> profile = data.get(dataKey) instanceof Map<?, ?> nested ? nested : data;
            Object value = profile.get(idKey);
            if (value instanceof Number number && number.longValue() > 0) {
                return java.util.Optional.of(number.longValue());
            }
            if (value instanceof String text && !text.isBlank()) {
                return java.util.Optional.of(Long.parseLong(text));
            }
        } catch (RuntimeException ex) {
            log.debug("Could not resolve {} profile for auth user {}", dataKey, authUserId);
        }
        return java.util.Optional.empty();
    }
}
