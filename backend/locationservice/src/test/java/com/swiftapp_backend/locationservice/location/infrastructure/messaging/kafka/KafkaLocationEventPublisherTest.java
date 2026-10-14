package com.swiftapp_backend.locationservice.location.infrastructure.messaging.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KafkaLocationEventPublisherTest {
    @Test
    void confirmedRelayFailsHttpFallbackWhenBrokerRejectsSend() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
        Object event = new Object();
        when(kafka.send("location-updated-topic", "ride-location-7", event))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));

        KafkaLocationEventPublisher publisher = new KafkaLocationEventPublisher(kafka);

        assertThrows(IllegalStateException.class,
                () -> publisher.publishConfirmed("location-updated-topic", "ride-location-7", event));
    }
}
