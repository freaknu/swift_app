package com.swiftapp_backend.ratingservice.rating.infrastructure.messaging.kafka;

import com.swiftapp_backend.ratingservice.rating.application.service.RatingEventService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class KafkaRatingConsumerTest {
    private final RatingEventService events = mock(RatingEventService.class);
    private final KafkaRatingConsumer consumer = new KafkaRatingConsumer(events);

    @Test
    void consumesHeaderlessRideCompletedRecord() {
        Map<String, Object> value = headerlessValue("""
                {"rideId":17,"driverId":23,"riderId":31}
                """);
        Acknowledgment acknowledgment = mock(Acknowledgment.class);

        consumer.handleRideCompleted(record("ride.completed", value), acknowledgment);

        verify(events).handleRideCompleted(17L, 23L, 31L);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumesDriverAndUserCreatedRecords() {
        Acknowledgment driverAck = mock(Acknowledgment.class);
        Acknowledgment userAck = mock(Acknowledgment.class);

        consumer.handleDriverCreated(record("driver.created", headerlessValue("{" +
                "\"driverId\":23}")), driverAck);
        consumer.handleUserCreated(record("user.created", headerlessValue("{" +
                "\"userId\":31}")), userAck);

        verify(events).handleDriverCreated(23L);
        verify(events).handleUserCreated(31L);
        verify(driverAck).acknowledge();
        verify(userAck).acknowledge();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> headerlessValue(String json) {
        KafkaConsumerConfig config = new KafkaConsumerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "127.0.0.1:1");
        Map<String, Object> properties = config.consumerFactory().getConfigurationProperties();
        assertEquals("java.util.HashMap", properties.get(JsonDeserializer.VALUE_DEFAULT_TYPE));
        JsonDeserializer<Map<String, Object>> deserializer = new JsonDeserializer<>();
        deserializer.configure(properties, false);
        try {
            return deserializer.deserialize("test", json.getBytes(StandardCharsets.UTF_8));
        } finally {
            deserializer.close();
        }
    }

    private static ConsumerRecord<String, Map<String, Object>> record(
            String topic, Map<String, Object> value) {
        return new ConsumerRecord<>(topic, 0, 0, "key", value);
    }
}
