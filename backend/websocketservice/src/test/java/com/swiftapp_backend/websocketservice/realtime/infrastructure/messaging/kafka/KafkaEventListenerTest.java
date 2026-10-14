package com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.websocketservice.realtime.application.service.RealtimeEventService;
import com.swiftapp_backend.websocketservice.realtime.domain.event.LocationUpdateEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RideMatchFailedEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RideOfferEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RealtimeEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.enums.RideStatus;
import com.swiftapp_backend.websocketservice.realtime.domain.model.NearbyDriver;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;

class KafkaEventListenerTest {

    @Test
    void relaysAuthoritativeAssignmentOnlyOnceAcrossKafkaAliases() {
        RealtimeEventService realtime = mock(RealtimeEventService.class);
        Acknowledgment ack = mock(Acknowledgment.class);
        KafkaEventListener listener = new KafkaEventListener(realtime, new ObjectMapper());
        Map<String, Object> assignment = Map.of("rideId", 101L, "riderId", 501L,
                "driverId", 11L, "rideStatus", "DRIVER_ASSIGNED");
        listener.consume(new ConsumerRecord<>(KafkaTopics.RIDE_ASSIGNED, 0, 0,
                "ride-101", assignment), ack);
        listener.consume(new ConsumerRecord<>(KafkaTopics.RIDE_ASSIGNED_DOT, 0, 0,
                "ride-101", assignment), ack);
        listener.consume(new ConsumerRecord<>(KafkaTopics.DRIVER_ASSIGNED_DOT, 0, 0,
                "ride-101", assignment), ack);

        verify(realtime, times(1)).publishRideUpdate(
                org.mockito.ArgumentMatchers.any(RealtimeEvent.class));
        verify(ack, times(3)).acknowledge();
    }

    @Test
    void doesNotFabricateAssignmentFromPrePersistenceMatch() {
        RealtimeEventService realtime = mock(RealtimeEventService.class);
        Acknowledgment ack = mock(Acknowledgment.class);
        KafkaEventListener listener = new KafkaEventListener(realtime, new ObjectMapper());
        listener.consume(new ConsumerRecord<>(KafkaTopics.DRIVER_MATCHED, 0, 0,
                "ride-101", Map.of("rideId", 101L, "driverId", 11L,
                        "authUserId", 501L)), ack);

        verifyNoInteractions(realtime);
        verify(ack).acknowledge();
    }

    @Test
    void sendsNoDriverFailureToRiderAndAcknowledgesRecord() {
        RealtimeEventService realtimeEventService = mock(RealtimeEventService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        KafkaEventListener listener = new KafkaEventListener(realtimeEventService, new ObjectMapper());
        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                KafkaTopics.RIDE_MATCH_FAILED, 0, 0, "ride-101",
                Map.of("rideId", 101L, "riderId", 501L, "reason", "NO_AVAILABLE_DRIVER"));

        listener.consume(record, acknowledgment);

        ArgumentCaptor<RealtimeEvent> eventCaptor = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtimeEventService).publishRideUpdate(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(RideMatchFailedEvent.class);
        RideMatchFailedEvent failed = (RideMatchFailedEvent) eventCaptor.getValue();
        assertThat(failed.getRideId()).isEqualTo(101L);
        assertThat(failed.getRiderId()).isEqualTo(501L);
        assertThat(failed.getRideStatus()).isEqualTo(RideStatus.MATCH_FAILED);
        assertThat(failed.getMessage()).contains("Please try again");
        verify(acknowledgment).acknowledge();
    }

    @Test
    void offerPushContainsCanonicalVehicleTypeFromMatchingBroadcast() {
        RealtimeEventService realtime = mock(RealtimeEventService.class);
        Acknowledgment ack = mock(Acknowledgment.class);
        KafkaEventListener listener = new KafkaEventListener(realtime, new ObjectMapper());
        Map<String, Object> payload = Map.of(
                "rideId", 101L, "riderId", 501L, "vehicleType", "HATCHBACK",
                "targets", List.of(Map.of("driverId", 11L, "distanceKm", 0.4)),
                "expiresAt", "2030-01-01T00:00:00Z");

        listener.consume(new ConsumerRecord<>(KafkaTopics.RIDE_OFFER_BROADCAST, 0, 0,
                "ride-101", payload), ack);

        ArgumentCaptor<RealtimeEvent> captured = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime, times(2)).publishRideUpdate(captured.capture());
        RideOfferEvent offer = captured.getAllValues().stream()
                .filter(RideOfferEvent.class::isInstance).map(RideOfferEvent.class::cast)
                .findFirst().orElseThrow();
        assertThat(offer.getRideTypeCode()).isEqualTo("HATCHBACK");
        assertThat(offer.getDriverId()).isEqualTo(11L);
        verify(ack).acknowledge();
    }

    @Test
    void relaysLocationUpdateAndAcknowledgesRecord() {
        RealtimeEventService realtimeEventService = mock(RealtimeEventService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        KafkaEventListener listener = new KafkaEventListener(realtimeEventService, new ObjectMapper());
        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                KafkaTopics.LOCATION_UPDATED,
                0,
                0,
                "ride-101",
                Map.of("rideId", 101L, "driverId", 11L, "latitude", 28.6139, "longitude", 77.2090)
        );

        listener.consume(record, acknowledgment);

        ArgumentCaptor<RealtimeEvent> eventCaptor = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtimeEventService).publishRideUpdate(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(LocationUpdateEvent.class);
        assertThat(((LocationUpdateEvent) eventCaptor.getValue()).getRideId()).isEqualTo(101L);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void relaysEveryBroadcastDriverAndAcknowledgesRecord() {
        RealtimeEventService realtimeEventService = mock(RealtimeEventService.class);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);
        KafkaEventListener listener = new KafkaEventListener(realtimeEventService, new ObjectMapper());
        ConsumerRecord<String, Object> record = new ConsumerRecord<>(
                KafkaTopics.DRIVER_BROADCAST,
                0,
                0,
                "drivers",
                List.of(Map.of(
                        "driverId", 11L,
                        "nearByPlace", "Delhi",
                        "latitude", 28.6139,
                        "longitude", 77.2090
                ))
        );

        listener.consume(record, acknowledgment);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<NearbyDriver>> driversCaptor = ArgumentCaptor.forClass(List.class);
        verify(realtimeEventService).publishDriverUpdates(driversCaptor.capture());
        assertThat(driversCaptor.getValue())
                .singleElement()
                .extracting(NearbyDriver::getDriverId)
                .isEqualTo(11L);
        verify(acknowledgment).acknowledge();
    }
}
