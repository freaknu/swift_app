package com.swiftapp_backend.riderservice.ride.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideDto;
import com.swiftapp_backend.riderservice.ride.application.service.RideDriverService;
import com.swiftapp_backend.riderservice.ride.application.service.RideService;
import com.swiftapp_backend.riderservice.ride.domain.event.RideDriverAcceptResponseEvent;
import com.swiftapp_backend.riderservice.shared.ApiException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KafkaConsumerServiceTest {

    @Test
    void matchedEventAssignsRideWithoutPersistingIncompleteDriverSnapshot() {
        RecordingRideService rides = new RecordingRideService();
        RecordingRideDriverService drivers = new RecordingRideDriverService();
        KafkaConsumerService consumer = new KafkaConsumerService(new ObjectMapper(), drivers, rides);
        Map<String, Object> payload = new HashMap<>();
        payload.put("rideId", 11);
        payload.put("driverId", 22);
        payload.put("vehicleId", 33);
        payload.put("authUserId", 44);
        payload.put("driverName", null);
        AtomicBoolean acked = new AtomicBoolean();

        consumer.publish(new ConsumerRecord<>("driver.matched", 0, 0L, "11", payload),
                () -> acked.set(true));

        assertThat(acked).isTrue();
        assertThat(rides.rideId).isEqualTo(11L);
        assertThat(rides.driverId).isEqualTo(22L);
        assertThat(rides.vehicleId).isEqualTo(33L);
        assertThat(drivers.saved).isZero();
    }

    @Test
    void legacyBroadcastIsNotSubscribedAndArrayPayloadIsAcknowledged() throws Exception {
        KafkaListener listener = KafkaConsumerService.class
                .getMethod("publish", ConsumerRecord.class, Acknowledgment.class)
                .getAnnotation(KafkaListener.class);
        assertThat(Arrays.asList(listener.topics())).doesNotContain("driver-broadcast-topic");

        RecordingRideService rides = new RecordingRideService();
        RecordingRideDriverService drivers = new RecordingRideDriverService();
        KafkaConsumerService consumer = new KafkaConsumerService(new ObjectMapper(), drivers, rides);
        AtomicBoolean acked = new AtomicBoolean();
        consumer.publish(new ConsumerRecord<>("ride-driver-topic", 0, 0L, "11", List.of()),
                () -> acked.set(true));

        assertThat(acked).isTrue();
        assertThat(drivers.saved).isZero();
        assertThat(rides.rideId).isNull();
    }

    @Test
    void completeLegacyDriverDetailCanStillBeSaved() {
        RecordingRideDriverService drivers = new RecordingRideDriverService();
        KafkaConsumerService consumer = new KafkaConsumerService(
                new ObjectMapper(), drivers, new RecordingRideService());
        AtomicBoolean acked = new AtomicBoolean();

        consumer.publish(new ConsumerRecord<>("ride-driver-topic", 0, 0L, "11",
                        Map.of("rideId", 11, "driverId", 22, "authUserId", 44,
                                "driverName", "Alex")),
                () -> acked.set(true));

        assertThat(acked).isTrue();
        assertThat(drivers.saved).isEqualTo(1);
    }

    @Test
    void staleOrIncompleteMatchingFailuresAreAcknowledgedOnce() {
        RecordingRideService rides = new RecordingRideService();
        KafkaConsumerService consumer = new KafkaConsumerService(
                new ObjectMapper(), new RecordingRideDriverService(), rides);
        AtomicBoolean missingIdentityAcked = new AtomicBoolean();
        consumer.publish(new ConsumerRecord<>("ride.match.failed", 0, 1L, "11",
                        Map.of("rideId", 11, "reason", "NO_AVAILABLE_DRIVER")),
                () -> missingIdentityAcked.set(true));
        assertThat(missingIdentityAcked).isTrue();
        assertThat(rides.failedRideId).isNull();

        rides.missingRide = true;
        AtomicBoolean absentRideAcked = new AtomicBoolean();
        consumer.publish(new ConsumerRecord<>("ride.match.failed", 0, 2L, "11",
                        Map.of("rideId", 11, "riderId", 44,
                                "reason", "NO_AVAILABLE_DRIVER")),
                () -> absentRideAcked.set(true));
        assertThat(absentRideAcked).isTrue();
        assertThat(rides.failedRideId).isEqualTo(11L);
        assertThat(rides.failedRiderId).isEqualTo(44L);
    }

    @Test
    void capturedPaymentAcknowledgesOnlyAfterPersistenceAndRetriesFailures() {
        RecordingRideService rides = new RecordingRideService();
        KafkaConsumerService consumer = new KafkaConsumerService(new ObjectMapper(),
                new RecordingRideDriverService(), rides);
        Map<String, Object> payload = new HashMap<>(Map.of("rideId", 11, "riderId", 44,
                "driverId", 22, "amount", 12500, "currency", "INR",
                "paymentStatus", "COMPLETED", "razorpayPaymentId", "pay_captured"));
        AtomicBoolean acked = new AtomicBoolean();
        rides.paymentFailure = true;
        assertThatThrownBy(() -> consumer.consumePayment(
                new ConsumerRecord<>("payment.completed", 0, 0L, "11", payload), () -> acked.set(true)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(acked).isFalse();
        rides.paymentFailure = false;
        consumer.consumePayment(new ConsumerRecord<>("payment.completed", 0, 0L, "11", payload),
                () -> acked.set(true));
        assertThat(acked).isTrue();
        assertThat(rides.paidRideId).isEqualTo(11L);
        acked.set(false);
        payload.put("amount", 12500.99);
        assertThatThrownBy(() -> consumer.consumePayment(
                new ConsumerRecord<>("payment.completed", 0, 1L, "11", payload), () -> acked.set(true)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(acked).isFalse();
    }

    private static final class RecordingRideService extends RideService {
        private Long rideId;
        private Long driverId;
        private Long vehicleId;
        private Long failedRideId;
        private Long failedRiderId;
        private boolean missingRide;
        private boolean paymentFailure;
        private Long paidRideId;

        private RecordingRideService() {
            super(null, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public RideDto assignMatchedDriver(Long rideId, Long driverId, Long vehicleId) {
            this.rideId = rideId;
            this.driverId = driverId;
            this.vehicleId = vehicleId;
            return null;
        }

        @Override
        public RideDto failMatching(Long rideId, Long riderId, String reason) {
            this.failedRideId = rideId;
            this.failedRiderId = riderId;
            if (missingRide) throw ApiException.notFound("Ride not found: " + rideId);
            return null;
        }

        @Override
        public void recordPaymentCompleted(Long rideId, Long riderId, Long driverId,
                                           Long amount, String currency, String paymentId) {
            if (paymentFailure) throw new IllegalStateException("database unavailable");
            paidRideId = rideId;
        }
    }

    private static final class RecordingRideDriverService extends RideDriverService {
        private int saved;

        private RecordingRideDriverService() {
            super(null, null);
        }

        @Override
        public void saveRideDriver(RideDriverAcceptResponseEvent data) {
            saved++;
        }
    }
}
