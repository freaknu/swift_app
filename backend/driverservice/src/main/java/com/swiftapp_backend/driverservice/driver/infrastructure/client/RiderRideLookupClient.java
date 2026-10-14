package com.swiftapp_backend.driverservice.driver.infrastructure.client;

import com.swiftapp_backend.driverservice.driver.application.port.RiderRideLookupPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Rebuilds active ride state from riderservice after a driver app reconnect. */
@Slf4j
@Component
public class RiderRideLookupClient implements RiderRideLookupPort {
    private final RestClient client;

    public RiderRideLookupClient(@Value("${services.rider-url:http://localhost:8083}") String riderUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2000);
        requestFactory.setReadTimeout(3000);
        client = RestClient.builder().baseUrl(riderUrl)
                .requestFactory(requestFactory).build();
    }

    @Override
    public Long activeRideId(Long driverId) {
        try {
            return client.get().uri("/internal/rides/drivers/{driverId}/active-id", driverId)
                    .retrieve().body(Long.class);
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Could not query rider service for active ride of driver " + driverId, ex);
        }
    }
}
