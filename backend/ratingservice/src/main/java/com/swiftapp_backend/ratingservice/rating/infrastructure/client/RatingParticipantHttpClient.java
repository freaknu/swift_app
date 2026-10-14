package com.swiftapp_backend.ratingservice.rating.infrastructure.client;

import com.swiftapp_backend.ratingservice.rating.application.port.RatingParticipantPort;
import com.swiftapp_backend.ratingservice.rating.domain.enums.UserType;
import com.swiftapp_backend.ratingservice.shared.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class RatingParticipantHttpClient implements RatingParticipantPort {
    private final RestClient userClient;
    private final RestClient driverClient;
    private final RestClient rideClient;

    public RatingParticipantHttpClient(
            @Value("${services.user-url:http://localhost:8082}") String userUrl,
            @Value("${services.driver-url:http://localhost:8096}") String driverUrl,
            @Value("${services.rider-url:${services.ride-url:http://localhost:8083}}") String rideUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        userClient = RestClient.builder().baseUrl(userUrl).requestFactory(factory).build();
        driverClient = RestClient.builder().baseUrl(driverUrl).requestFactory(factory).build();
        rideClient = RestClient.builder().baseUrl(rideUrl).requestFactory(factory).build();
    }

    @Override
    public Ride ride(Long rideId) {
        try {
            Map<?, ?> data = data(rideClient.get().uri("/internal/rides/{rideId}", rideId)
                    .retrieve().body(Map.class));
            return new Ride(id(data, "id"), id(data, "riderId"),
                    id(data, "driverId"), text(data, "currentStatus"));
        } catch (HttpClientErrorException.NotFound ex) {
            throw ApiException.notFound("Ride was not found");
        }
    }

    @Override
    public Long profileIdForAuthUser(Long authUserId, UserType role) {
        try {
            if (role == UserType.RIDER) {
                Map<?, ?> data = data(userClient.get().uri("/api/v1/user/getUserDetails")
                        .header("x-user-id", authUserId.toString()).retrieve().body(Map.class));
                if (!authUserId.equals(id(data, "authUserId"))) {
                    throw new IllegalStateException("User identity response did not match caller");
                }
                return id(data, "id");
            }
            Map<?, ?> data = data(driverClient.get().uri("/api/v1/driver/driverMiniDetail")
                    .header("x-user-id", authUserId.toString()).retrieve().body(Map.class));
            return id(data, "driverId");
        } catch (HttpClientErrorException.NotFound ex) {
            throw ApiException.forbidden("Authenticated participant profile is unavailable");
        }
    }

    @Override
    public Long authUserIdForProfile(Long profileId, UserType role) {
        RestClient client = role == UserType.RIDER ? userClient : driverClient;
        String path = role == UserType.RIDER
                ? "/internal/profiles/users/{profileId}"
                : "/internal/profiles/drivers/{profileId}";
        try {
            return id(data(client.get().uri(path, profileId).retrieve().body(Map.class)), "authUserId");
        } catch (HttpClientErrorException.NotFound ex) {
            throw ApiException.notFound("Rating recipient was not found");
        }
    }

    private static Map<?, ?> data(Map<?, ?> response) {
        if (response == null || !(response.get("data") instanceof Map<?, ?> data)) {
            throw new IllegalStateException("Profile or ride service returned no data");
        }
        return data;
    }

    private static Long id(Map<?, ?> data, String key) {
        Object raw = data.get(key);
        if (raw instanceof Number number) return number.longValue();
        if (raw instanceof String value && !value.isBlank()) return Long.valueOf(value);
        throw new IllegalStateException("Missing " + key + " in profile or ride response");
    }

    private static String text(Map<?, ?> data, String key) {
        Object value = data.get(key);
        return value == null ? null : value.toString();
    }
}
