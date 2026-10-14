package com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.websocketservice.realtime.application.service.RealtimeEventService;
import com.swiftapp_backend.websocketservice.realtime.domain.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.PAYMENT_COMPLETED;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.PAYMENT_FAILED;
import static com.swiftapp_backend.websocketservice.realtime.infrastructure.messaging.kafka.KafkaTopics.PAYMENT_ORDER_CREATED;

@Service
@RequiredArgsConstructor
public class KafkaPaymentEventListener {
    private final RealtimeEventService realtimeEventService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = {PAYMENT_ORDER_CREATED, PAYMENT_COMPLETED, PAYMENT_FAILED},
            groupId = "websocket-service",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, Object> record, Acknowledgment acknowledgment) {
        String status = switch (record.topic()) {
            case PAYMENT_ORDER_CREATED -> "ORDER_CREATED";
            case PAYMENT_COMPLETED -> "COMPLETED";
            case PAYMENT_FAILED -> "FAILED";
            default -> throw new IllegalArgumentException("Unsupported payment topic");
        };
        JsonNode payload = objectMapper.valueToTree(record.value());
        if (payload == null || !payload.isObject()) {
            throw new IllegalArgumentException("Payment event must be a JSON object");
        }
        if (!status.equals(requiredText(payload, "paymentStatus"))) {
            throw new IllegalArgumentException("Payment status does not match its topic");
        }
        String eventTime = requiredText(payload, "eventTime");
        Instant.parse(eventTime);
        String currency = requiredText(payload, "currency");
        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Payment currency must be an ISO currency code");
        }
        String orderId = optionalText(payload, "razorpayOrderId");
        String paymentId = optionalText(payload, "razorpayPaymentId");
        if (!"FAILED".equals(status) && orderId == null) {
            throw new IllegalArgumentException("Payment order ID is required");
        }
        if ("COMPLETED".equals(status) && paymentId == null) {
            throw new IllegalArgumentException("Completed payment ID is required");
        }

        // Construct a whitelist DTO rather than forwarding the incoming map.
        // Payment state deliberately has no rideStatus, so it cannot change
        // the ride lifecycle in clients that share these subscription topics.
        PaymentEvent event = new PaymentEvent(
                requiredText(payload, "eventId"),
                "PAYMENT_" + status,
                positiveInteger(payload, "rideId"),
                positiveInteger(payload, "riderId"),
                positiveInteger(payload, "driverId"),
                status,
                orderId,
                paymentId,
                positiveInteger(payload, "amount"),
                currency,
                eventTime
        );
        // Redis publication errors propagate to Kafka. Never acknowledge a
        // failed relay; the dedicated listener factory retries the record.
        realtimeEventService.publishRideUpdate(event);
        acknowledgment.acknowledge();
    }

    private static Long positiveInteger(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || !value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() <= 0) {
            throw new IllegalArgumentException("Payment " + field + " must be a positive integer");
        }
        return value.longValue();
    }

    private static String requiredText(JsonNode payload, String field) {
        String value = optionalText(payload, field);
        if (value == null) {
            throw new IllegalArgumentException("Payment " + field + " is required");
        }
        return value;
    }

    private static String optionalText(JsonNode payload, String field) {
        JsonNode value = payload.get(field);
        if (value == null || value.isNull()) return null;
        if (!value.isTextual() || value.textValue().isBlank()) {
            throw new IllegalArgumentException("Payment " + field + " must be nonempty text");
        }
        return value.textValue();
    }
}
