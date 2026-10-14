package com.swiftapp_backend.analyticalservice.analytics.infrastructure.client;

import com.swiftapp_backend.analyticalservice.analytics.application.port.AnalyticsProfilePort;
import com.swiftapp_backend.analyticalservice.shared.AnalyticsAccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class AnalyticsProfileHttpClient implements AnalyticsProfilePort {
    private final RestClient userClient;
    private final RestClient driverClient;

    public AnalyticsProfileHttpClient(
            @Value("${services.user-url:http://localhost:8082}") String userUrl,
            @Value("${services.driver-url:http://localhost:8096}") String driverUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        userClient = RestClient.builder().baseUrl(userUrl).requestFactory(factory).build();
        driverClient = RestClient.builder().baseUrl(driverUrl).requestFactory(factory).build();
    }

    @Override
    public Long riderAuthUserId(Long riderId) {
        return lookup(userClient, "/internal/profiles/users/{profileId}", riderId);
    }

    @Override
    public Long driverAuthUserId(Long driverId) {
        return lookup(driverClient, "/internal/profiles/drivers/{profileId}", driverId);
    }

    private Long lookup(RestClient client, String path, Long profileId) {
        if (profileId == null || profileId <= 0) {
            throw new AnalyticsAccessDeniedException("Invalid analytics profile");
        }
        try {
            Map<?, ?> response = client.get().uri(path, profileId).retrieve().body(Map.class);
            if (response == null || !(response.get("data") instanceof Map<?, ?> data)) {
                throw new IllegalStateException("Profile lookup returned no data");
            }
            Object raw = data.get("authUserId");
            if (raw instanceof Number value) return value.longValue();
            if (raw instanceof String value && !value.isBlank()) return Long.valueOf(value);
            throw new IllegalStateException("Profile lookup returned no auth identity");
        } catch (HttpClientErrorException ex) {
            throw new AnalyticsAccessDeniedException("Analytics profile was not found");
        }
    }
}
