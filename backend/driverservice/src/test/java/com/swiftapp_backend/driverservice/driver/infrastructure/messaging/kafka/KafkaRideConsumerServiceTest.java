package com.swiftapp_backend.driverservice.driver.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.driverservice.driver.application.service.DriverOfferService;
import com.swiftapp_backend.driverservice.driver.application.service.DriverStaticsService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;

class KafkaRideConsumerServiceTest {

    @Test
    void subscribesOnlyToCanonicalTerminalTopicsToAvoidHistoricalAliasReplay() throws Exception {
        KafkaListener listener = KafkaRideConsumerService.class
                .getMethod("consume", ConsumerRecord.class, Acknowledgment.class)
                .getAnnotation(KafkaListener.class);

        assertThat(listener.topics()).contains("ride.completed", "ride.cancelled")
                .doesNotContain("ride-completed", "ride-cancelled");
    }

    @Test
    void failedRideStatisticsIsNotAcknowledgedSoKafkaCanRetry() {
        DriverOfferService offers = mock(DriverOfferService.class);
        DriverStaticsService statistics = mock(DriverStaticsService.class);
        KafkaRideConsumerService consumer = new KafkaRideConsumerService(
                new ObjectMapper(), statistics, offers);
        Acknowledgment ack = mock(Acknowledgment.class);
        doThrow(new IllegalStateException("database unavailable"))
                .when(statistics).recordRideCompleted(91004L, 102L, 120.0, 4.5);

        assertThrows(IllegalStateException.class, () -> consumer.consume(
                new ConsumerRecord<>("ride.completed", 0, 0L, "91004",
                        Map.of("rideId", 91004, "driverId", 102,
                                "totalFare", 120.0, "actualDistance", 4.5)), ack));

        verify(ack, never()).acknowledge();
    }

    @Test
    void headerlessCompletionAndCancellationMapsClearActiveRide() {
        DriverOfferService offers = mock(DriverOfferService.class);
        DriverStaticsService statistics = mock(DriverStaticsService.class);
        KafkaRideConsumerService consumer = new KafkaRideConsumerService(
                new ObjectMapper(), statistics, offers);
        Acknowledgment completedAck = mock(Acknowledgment.class);
        Acknowledgment cancelledAck = mock(Acknowledgment.class);
        Map<String, Object> event = Map.of("rideId", 91004, "driverId", 102,
                "totalFare", 120.0, "actualDistance", 4.5);

        consumer.consume(new ConsumerRecord<>("ride-completed", 0, 0L, "91004", event),
                completedAck);
        consumer.consume(new ConsumerRecord<>("ride-cancelled", 0, 1L, "91004", event),
                cancelledAck);

        verify(offers, org.mockito.Mockito.times(2)).clearActiveRide(102L, 91004L);
        verify(statistics).recordRideCompleted(91004L, 102L, 120.0, 4.5);
        verify(statistics).recordRideCancelled(91004L, 102L);
        verify(completedAck).acknowledge();
        verify(cancelledAck).acknowledge();
    }

    @Test
    void rideIdFallsBackToKafkaKeyAndMissingIdentityIsNotAcknowledged() {
        DriverOfferService offers = mock(DriverOfferService.class);
        DriverStaticsService statistics = mock(DriverStaticsService.class);
        KafkaRideConsumerService consumer = new KafkaRideConsumerService(
                new ObjectMapper(), statistics, offers);
        Acknowledgment validAck = mock(Acknowledgment.class);
        Acknowledgment missingAck = mock(Acknowledgment.class);

        consumer.consume(new ConsumerRecord<>("ride.completed", 0, 0L, "91004",
                Map.of("driverId", 102, "totalFare", 120.0, "actualDistance", 4.5)), validAck);
        verify(statistics).recordRideCompleted(91004L, 102L, 120.0, 4.5);
        verify(validAck).acknowledge();

        assertThrows(IllegalStateException.class, () -> consumer.consume(
                new ConsumerRecord<>("ride.cancelled", 0, 1L, "invalid", Map.of("driverId", 102)),
                missingAck));
        verify(missingAck, never()).acknowledge();
    }
}
