package com.swiftapp_backend.paymentservice.payment.infrastructure.client;
import com.fasterxml.jackson.databind.JsonNode;
import com.swiftapp_backend.paymentservice.payment.application.port.RideLookupPort;
import com.swiftapp_backend.paymentservice.payment.domain.exception.PaymentException;
import com.swiftapp_backend.paymentservice.payment.domain.model.CompletedRide;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
@Component
public class RideLookupHttpClient implements RideLookupPort {
    private final RestClient rides;
    private final RestClient users;
    public RideLookupHttpClient(@Value("${services.rider-url:http://localhost:8083}") String riderUrl,
                                @Value("${services.user-url:http://localhost:8082}") String userUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000); factory.setReadTimeout(5000);
        rides = RestClient.builder().baseUrl(riderUrl).requestFactory(factory).build();
        users = RestClient.builder().baseUrl(userUrl).requestFactory(factory).build();
    }
    @Override public CompletedRide getParticipantRide(Long rideId, Long authUserId) {
        JsonNode data = get(rides, "/api/v1/ride/getRideDetail/" + rideId, authUserId);
        if (data.path("id").asLong() != rideId) throw unavailable();
        JsonNode fare = data.path("fareDetail").path("totalFare");
        return new CompletedRide(rideId, data.path("riderId").asLong(), data.path("driverId").asLong(),
            data.path("isCompleted").asBoolean() && "COMPLETED".equals(data.path("currentStatus").asText()),
            fare.isNumber() ? fare.decimalValue() : null, data.path("fareDetail").path("currency").asText(), data.path("completedAt").asText());
    }
    @Override public Long riderIdForAuthUser(Long authUserId) {
        JsonNode data = get(users, "/api/v1/user/getUserDetails", authUserId);
        if (data.path("authUserId").asLong() != authUserId) throw new PaymentException(PaymentException.Kind.FORBIDDEN, "Authenticated rider does not match profile");
        long id = data.path("id").asLong();
        if (id <= 0) throw new PaymentException(PaymentException.Kind.FORBIDDEN, "A rider profile is required");
        return id;
    }
    private JsonNode get(RestClient client, String path, Long authUserId) {
        if (authUserId == null || authUserId <= 0) throw new PaymentException(PaymentException.Kind.FORBIDDEN, "Authenticated user is required");
        try {
            JsonNode response = client.get().uri(path).header("x-user-id", authUserId.toString()).retrieve().body(JsonNode.class);
            if (response == null || !response.path("data").isObject()) throw unavailable();
            return response.path("data");
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 403 || ex.getStatusCode().value() == 401) throw new PaymentException(PaymentException.Kind.FORBIDDEN, "You cannot access this ride or rider profile");
            if (ex.getStatusCode().value() == 404) throw new PaymentException(PaymentException.Kind.NOT_FOUND, "Ride or rider profile not found");
            throw unavailable();
        } catch (RestClientException ex) { throw unavailable(); }
    }
    private PaymentException unavailable() { return new PaymentException(PaymentException.Kind.UNAVAILABLE, "Ride identity service is temporarily unavailable"); }
}
