package com.swiftapp_backend.websocketservice.realtime.infrastructure.config;

import com.swiftapp_backend.websocketservice.realtime.api.dto.ChatSessionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RideChatSessionClientTest {
    private MockRestServiceServer rides;
    private MockRestServiceServer users;
    private DriverIdentityClient driverIdentity;
    private RideChatSessionClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder rideBuilder = RestClient.builder().baseUrl("http://ride");
        RestClient.Builder userBuilder = RestClient.builder().baseUrl("http://user");
        rides = MockRestServiceServer.bindTo(rideBuilder).build();
        users = MockRestServiceServer.bindTo(userBuilder).build();
        driverIdentity = mock(DriverIdentityClient.class);
        client = new RideChatSessionClient(rideBuilder.build(), userBuilder.build(), driverIdentity);
    }

    @Test
    void assignedDriverUsesAuthoritativeStatusInsteadOfClientStatus() {
        expectRide("STARTED");
        when(driverIdentity.belongsTo(42L, 11L)).thenReturn(true);
        ChatSessionDto request = session();
        request.setRideStatus("COMPLETED");

        RideChatSessionClient.Context context = client.validate(42L, request);

        assertEquals(new RideChatSessionClient.Context(101L, 201L, 11L, "STARTED"), context);
        rides.verify();
        users.verify();
    }

    @Test
    void assignedRiderCanBootstrapUsingTheirResolvedProfileIdentity() {
        expectRide("DRIVER_ASSIGNED");
        expectRiderProfile(201L);

        assertEquals(201L, client.validate(42L, session()).riderId());
        rides.verify();
        users.verify();
    }

    @Test
    void aNonParticipantCannotBootstrapAnExistingRide() {
        expectRide("DRIVER_ASSIGNED");
        expectRiderProfile(999L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> client.validate(42L, session()));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        rides.verify();
        users.verify();
    }

    @Test
    void clientCannotReplaceTheActualRideParticipants() {
        expectRide("DRIVER_ASSIGNED");
        when(driverIdentity.belongsTo(42L, 11L)).thenReturn(true);
        ChatSessionDto request = session();
        request.setRiderId(999L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> client.validate(42L, request));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        rides.verify();
        users.verify();
    }

    @Test
    void closedRideCannotBeReopenedByClaimingAnActiveStatus() {
        expectRide("CANCELLED");
        ChatSessionDto request = session();
        request.setRideStatus("DRIVER_ASSIGNED");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> client.validate(42L, request));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        rides.verify();
    }

    @Test
    void inventedRideCannotCreateAChatSession() {
        rides.expect(requestTo("http://ride/api/v1/ride/getRideDetail/101"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> client.validate(42L, session()));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        rides.verify();
    }

    private void expectRide(String status) {
        rides.expect(requestTo("http://ride/api/v1/ride/getRideDetail/101"))
                .andExpect(header("x-user-id", "42"))
                .andRespond(withSuccess("{\"data\":{\"id\":101,\"riderId\":201,\"driverId\":11,"
                        + "\"currentStatus\":\"" + status + "\"}}", MediaType.APPLICATION_JSON));
    }

    private void expectRiderProfile(Long riderId) {
        users.expect(requestTo("http://user/api/v1/user/getUserDetails"))
                .andExpect(header("x-user-id", "42"))
                .andRespond(withSuccess("{\"data\":{\"id\":" + riderId + "}}", MediaType.APPLICATION_JSON));
    }

    private ChatSessionDto session() {
        return ChatSessionDto.builder().rideId(101L).riderId(201L).driverId(11L)
                .rideStatus("DRIVER_ASSIGNED").build();
    }
}
