package com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.websocketservice.realtime.application.service.RealtimeEventService;
import com.swiftapp_backend.websocketservice.realtime.domain.event.PaymentEvent;
import com.swiftapp_backend.websocketservice.realtime.domain.event.RealtimeEvent;
import com.swiftapp_backend.websocketservice.realtime.infrastructure.realtime.redis.RedisRealtimeMessagePublisher;
import com.swiftapp_backend.websocketservice.realtime.infrastructure.realtime.redis.RedisSubscriber;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class KafkaPaymentEventListenerTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RealtimeEventService realtime = mock(RealtimeEventService.class);
    private final Acknowledgment acknowledgment = mock(Acknowledgment.class);
    private final KafkaPaymentEventListener listener = new KafkaPaymentEventListener(realtime, objectMapper);

    @ParameterizedTest
    @CsvSource({
            "payment.order.created,ORDER_CREATED,PAYMENT_ORDER_CREATED",
            "payment.completed,COMPLETED,PAYMENT_COMPLETED",
            "payment.failed,FAILED,PAYMENT_FAILED"
    })
    void relaysEachPaymentStateBeforeAcknowledging(String topic, String status, String eventType) {
        listener.consume(record(topic, payload(status)), acknowledgment);

        ArgumentCaptor<RealtimeEvent> captured = ArgumentCaptor.forClass(RealtimeEvent.class);
        var ordered = inOrder(realtime, acknowledgment);
        ordered.verify(realtime).publishRideUpdate(captured.capture());
        ordered.verify(acknowledgment).acknowledge();
        PaymentEvent event = (PaymentEvent) captured.getValue();
        assertThat(event.eventType()).isEqualTo(eventType);
        assertThat(event.paymentStatus()).isEqualTo(status);
        assertThat(event.rideId()).isEqualTo(101L);
        assertThat(event.riderId()).isEqualTo(501L);
        assertThat(event.driverId()).isEqualTo(11L);
        assertThat(event.amount()).isEqualTo(12500L);
        assertThat(event.currency()).isEqualTo("INR");
    }

    @Test
    void fansOutThroughRedisToOnlyTheRideParticipantsWithWhitelistedFields() {
        @SuppressWarnings("unchecked")
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        RealtimeEventService service = new RealtimeEventService(new RedisRealtimeMessagePublisher(redis));
        KafkaPaymentEventListener relay = new KafkaPaymentEventListener(service, objectMapper);
        Map<String, Object> payment = payload("COMPLETED");
        payment.put("razorpayKeySecret", "must-not-reach-clients");
        payment.put("razorpaySignature", "must-not-reach-clients");
        payment.put("rideStatus", "STARTED");
        payment.put("eventType", "RIDE_STARTED");

        relay.consume(record(KafkaTopics.PAYMENT_COMPLETED, payment), acknowledgment);

        ArgumentCaptor<Object> redisPayload = ArgumentCaptor.forClass(Object.class);
        verify(redis).convertAndSend(eq("ride-updates"), redisPayload.capture());
        Map<String, Object> outgoing = objectMapper.convertValue(redisPayload.getValue(), new TypeReference<>() {});
        assertThat(outgoing.keySet()).containsExactlyInAnyOrder(
                "eventId", "eventType", "rideId", "riderId", "driverId", "paymentStatus",
                "razorpayOrderId", "razorpayPaymentId", "amount", "currency", "eventTime");
        assertThat(outgoing.get("eventType")).isEqualTo("PAYMENT_COMPLETED");
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        new RedisSubscriber(messaging, objectMapper).receiveMessage(outgoing);
        verify(messaging).convertAndSend("/topic/rides/101", outgoing);
        verify(messaging).convertAndSend("/topic/riders/501", outgoing);
        verify(messaging).convertAndSend("/topic/drivers/11", outgoing);
        verifyNoMoreInteractions(messaging);
    }

    @ParameterizedTest
    @ValueSource(strings = {"eventId", "rideId", "riderId", "driverId", "amount", "currency", "eventTime",
            "paymentStatus", "razorpayOrderId", "razorpayPaymentId"})
    void rejectsIncompleteSuccessWithoutPublishingOrAcknowledging(String missingField) {
        Map<String, Object> payment = payload("COMPLETED");
        payment.remove(missingField);
        assertThatThrownBy(() -> listener.consume(record(KafkaTopics.PAYMENT_COMPLETED, payment), acknowledgment))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(realtime, acknowledgment);
    }

    @ParameterizedTest
    @ValueSource(strings = {"rideId", "riderId", "driverId", "amount"})
    void rejectsZeroAndFractionalIntegersWithoutAcknowledging(String field) {
        Map<String, Object> payment = payload("COMPLETED");
        for (Number invalid : new Number[]{0, -1, 12.5}) {
            payment.put(field, invalid);
            assertThatThrownBy(() -> listener.consume(record(KafkaTopics.PAYMENT_COMPLETED, payment), acknowledgment))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(realtime, acknowledgment);
    }

    @Test
    void rejectsMismatchedStatusAndMalformedRecords() {
        assertThatThrownBy(() -> listener.consume(record(KafkaTopics.PAYMENT_COMPLETED, payload("FAILED")), acknowledgment))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listener.consume(record(KafkaTopics.PAYMENT_COMPLETED, "not-json-object"), acknowledgment))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listener.consume(record(KafkaTopics.PAYMENT_COMPLETED, null), acknowledgment))
                .isInstanceOf(IllegalArgumentException.class);
        Map<String, Object> payment = payload("COMPLETED");
        payment.put("eventTime", "not-a-timestamp");
        assertThatThrownBy(() -> listener.consume(record(KafkaTopics.PAYMENT_COMPLETED, payment), acknowledgment))
                .isInstanceOf(RuntimeException.class);
        verifyNoInteractions(realtime, acknowledgment);
    }

    @Test
    void allowsOrderCreationFailuresWithoutGatewayIdentifiers() {
        Map<String, Object> payment = payload("FAILED");
        payment.remove("razorpayOrderId");
        payment.remove("razorpayPaymentId");
        listener.consume(record(KafkaTopics.PAYMENT_FAILED, payment), acknowledgment);
        verify(realtime).publishRideUpdate(any(PaymentEvent.class));
        verify(acknowledgment).acknowledge();
    }

    @Test
    void relayFailureRemainsUnacknowledgedAndTheSameRecordCanBeRetried() {
        doThrow(new IllegalStateException("Redis unavailable")).doNothing()
                .when(realtime).publishRideUpdate(any(PaymentEvent.class));
        ConsumerRecord<String, Object> record = record(KafkaTopics.PAYMENT_COMPLETED, payload("COMPLETED"));

        assertThatThrownBy(() -> listener.consume(record, acknowledgment))
                .isInstanceOf(IllegalStateException.class).hasMessage("Redis unavailable");
        verifyNoInteractions(acknowledgment);
        listener.consume(record, acknowledgment);

        verify(realtime, times(2)).publishRideUpdate(any(PaymentEvent.class));
        verify(acknowledgment).acknowledge();
    }

    private static ConsumerRecord<String, Object> record(String topic, Object value) {
        return new ConsumerRecord<>(topic, 0, 0, "ride-101", value);
    }

    private static Map<String, Object> payload(String status) {
        return new HashMap<>(Map.ofEntries(
                Map.entry("eventId", "payment-event-1"),
                Map.entry("rideId", 101L), Map.entry("riderId", 501L), Map.entry("driverId", 11L),
                Map.entry("paymentStatus", status),
                Map.entry("razorpayOrderId", "order_1"), Map.entry("razorpayPaymentId", "pay_1"),
                Map.entry("amount", 12500L), Map.entry("currency", "INR"),
                Map.entry("eventTime", "2026-10-13T10:00:00Z")
        ));
    }
}
