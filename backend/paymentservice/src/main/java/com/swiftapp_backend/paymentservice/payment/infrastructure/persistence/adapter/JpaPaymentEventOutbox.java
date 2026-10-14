package com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.adapter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftapp_backend.paymentservice.payment.application.port.PaymentEventPort;
import com.swiftapp_backend.paymentservice.payment.domain.event.PaymentStatusEvent;
import com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.entity.PaymentOutboxEntity;
import com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.repository.PaymentOutboxJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class JpaPaymentEventOutbox implements PaymentEventPort {
    private final PaymentOutboxJpaRepository outbox;
    private final ObjectMapper mapper;
    @Override public void enqueue(String topic, PaymentStatusEvent event) {
        try {
            PaymentOutboxEntity row = new PaymentOutboxEntity();
            row.setEventId(event.eventId()); row.setRideId(event.rideId()); row.setTopic(topic);
            row.setPayload(mapper.writeValueAsString(event)); row.setCreatedAt(event.eventTime());
            outbox.save(row);
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Cannot serialize payment event", ex); }
    }
}
