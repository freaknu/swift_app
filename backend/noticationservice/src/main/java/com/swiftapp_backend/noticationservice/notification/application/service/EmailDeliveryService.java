package com.swiftapp_backend.noticationservice.notification.application.service;

import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.notification.domain.model.EmailDelivery;
import com.swiftapp_backend.noticationservice.notification.domain.repository.EmailDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** A failed SMTP send rolls back its delivery marker, so Kafka can retry it. */
@Service
@RequiredArgsConstructor
public class EmailDeliveryService {
    private final EmailDeliveryRepository repository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deliver(String eventKey, Long recipientId, UserRole role, Runnable send) {
        EmailDelivery delivery = repository
                .findByEventKeyAndRecipientIdAndRecipientRole(eventKey, recipientId, role)
                .orElseGet(() -> {
                    EmailDelivery created = new EmailDelivery();
                    created.setEventKey(eventKey);
                    created.setRecipientId(recipientId);
                    created.setRecipientRole(role);
                    return repository.saveAndFlush(created);
                });
        if (delivery.isSent()) return;
        send.run();
        delivery.setSent(true);
        delivery.setSentAt(LocalDateTime.now());
        repository.save(delivery);
    }
}
