package com.swiftapp_backend.riderservice.ride.infrastructure.messaging.kafka;

import com.swiftapp_backend.riderservice.ride.application.port.MessagePublisherPort;
import com.swiftapp_backend.riderservice.ride.application.port.RideEventPublisherPort;
import com.swiftapp_backend.riderservice.ride.domain.event.RideEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class TransactionalRideEventPublisher implements RideEventPublisherPort {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final MessagePublisherPort messagePublisher;

    @Value("${app.kafka.ride-events-topic:ride-events}")
    private String topic;

    @Override
    public void publishAfterCommit(RideEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sendToKafka(RideEvent event) {
        messagePublisher.publish(topic, String.valueOf(event.rideId()), event);
    }
}
