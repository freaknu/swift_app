package com.swiftapp_backend.riderservice.ride.infrastructure.client;

import com.swiftapp_backend.riderservice.ride.application.port.RideIdentityPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class RideIdentityHttpClient implements RideIdentityPort {
    private final RestClient userClient;
    private final RestClient driverClient;

    public RideIdentityHttpClient(
            @Value("${services.user-url:http://localhost:8082}") String userUrl,
            @Value("${services.driver-url:http://localhost:8096}") String driverUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        this.userClient = RestClient.builder().baseUrl(userUrl).requestFactory(factory).build();
        this.driverClient = RestClient.builder().baseUrl(driverUrl).requestFactory(factory).build();
    }

    @Override
    public Long riderIdForAuthUser(Long authUserId) {
        Map<?, ?> response = userClient.get().uri("/api/v1/user/getUserDetails")
                .header("x-user-id", authUserId.toString()).retrieve().body(Map.class);
        Map<?, ?> data = data(response, "user");
        Long returnedAuthUserId = id(data, "authUserId");
        if (!authUserId.equals(returnedAuthUserId)) {
            throw new IllegalStateException("User service returned a different auth identity");
        }
        return id(data, "id");
    }

    @Override
    public Long driverIdForAuthUser(Long authUserId) {
        Map<?, ?> response = driverClient.get().uri("/api/v1/driver/driverMiniDetail")
                .header("x-user-id", authUserId.toString()).retrieve().body(Map.class);
        return id(data(response, "driver"), "driverId");
    }

    @Override
    public Long riderAuthUserId(Long riderId) {
        return id(data(userClient.get().uri("/internal/profiles/users/{id}", riderId)
                .retrieve().body(Map.class), "user"), "authUserId");
    }

    @Override
    public Long driverAuthUserId(Long driverId) {
        return id(data(driverClient.get().uri("/internal/profiles/drivers/{id}", driverId)
                .retrieve().body(Map.class), "driver"), "authUserId");
    }

    private static Map<?, ?> data(Map<?, ?> response, String service) {
        if (response == null || !(response.get("data") instanceof Map<?, ?> data)) {
            throw new IllegalStateException("Missing profile data from " + service + " service");
        }
        return data;
    }

    private static Long id(Map<?, ?> data, String key) {
        Object raw = data.get(key);
        if (raw instanceof Number number) return number.longValue();
        if (raw instanceof String value && !value.isBlank()) return Long.valueOf(value);
        throw new IllegalStateException("Missing " + key + " in identity response");
    }
}
