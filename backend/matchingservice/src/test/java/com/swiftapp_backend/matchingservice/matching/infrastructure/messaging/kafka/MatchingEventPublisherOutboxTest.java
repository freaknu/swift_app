package com.swiftapp_backend.matchingservice.matching.infrastructure.messaging.kafka;

import com.swiftapp_backend.matchingservice.matching.domain.events.DriverMatchedEvent;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverOfferRepository;
import com.swiftapp_backend.matchingservice.matching.domain.repository.MatchingStateRepository;
import com.swiftapp_backend.matchingservice.matching.domain.events.RideMatchFailedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchingEventPublisherOutboxTest {
    @Test
    void failedBrokerSendRemainsPendingAndRetryAcknowledgesIt() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
        DriverOfferRepository outbox = mock(DriverOfferRepository.class);
        DriverMatchedEvent matched = DriverMatchedEvent.builder()
                .rideId(5001L).driverId(101L).vehicleId(301L)
                .authUserId(7L).isActive(true).build();
        @SuppressWarnings("unchecked")
        SendResult<String, Object> sent = mock(SendResult.class);
        when(kafka.send("driver.matched", "5001", matched)).thenReturn(
                CompletableFuture.failedFuture(new IllegalStateException("broker down")),
                CompletableFuture.completedFuture(sent));
        when(outbox.pendingMatchedEvents(100)).thenReturn(List.of(matched));
        MatchingEventPublisher publisher = new MatchingEventPublisher(kafka, outbox,
                mock(MatchingStateRepository.class));

        publisher.publishDriverMatched(matched);
        verify(outbox, never()).acknowledgeMatchedEvent(5001L);

        publisher.retryPendingMatchedEvents();
        verify(outbox).acknowledgeMatchedEvent(5001L);
    }

    @Test
    void failedMatchEventRetriesUntilBrokerAcknowledgesIt() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, Object> kafka = mock(KafkaTemplate.class);
        MatchingStateRepository outbox = mock(MatchingStateRepository.class);
        RideMatchFailedEvent failed = new RideMatchFailedEvent(5002L, 7L,
                "NO_AVAILABLE_DRIVER");
        @SuppressWarnings("unchecked")
        SendResult<String, Object> sent = mock(SendResult.class);
        when(kafka.send("ride.match.failed", "5002", failed)).thenReturn(
                CompletableFuture.failedFuture(new IllegalStateException("broker down")),
                CompletableFuture.completedFuture(sent));
        when(outbox.pendingFailedEvents(100)).thenReturn(List.of(failed));
        MatchingEventPublisher publisher = new MatchingEventPublisher(kafka,
                mock(DriverOfferRepository.class), outbox);

        publisher.publishMatchFailed(5002L, 7L, "NO_AVAILABLE_DRIVER");
        verify(outbox, never()).acknowledgeFailedEvent(5002L);

        publisher.retryPendingFailedEvents();
        verify(outbox).acknowledgeFailedEvent(5002L);
    }
}
