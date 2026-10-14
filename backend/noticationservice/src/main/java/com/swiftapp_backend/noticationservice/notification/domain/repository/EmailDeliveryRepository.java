package com.swiftapp_backend.noticationservice.notification.domain.repository;

import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.notification.domain.model.EmailDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailDeliveryRepository extends JpaRepository<EmailDelivery, Long> {
    Optional<EmailDelivery> findByEventKeyAndRecipientIdAndRecipientRole(
            String eventKey, Long recipientId, UserRole role);
}
