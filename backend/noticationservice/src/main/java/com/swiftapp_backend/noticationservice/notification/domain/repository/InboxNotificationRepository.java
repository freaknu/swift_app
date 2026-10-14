package com.swiftapp_backend.noticationservice.notification.domain.repository;

import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.notification.domain.model.InboxNotification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InboxNotificationRepository extends JpaRepository<InboxNotification, Long> {
    Optional<InboxNotification> findByEventKeyAndRecipientIdAndRecipientRole(
            String eventKey, Long recipientId, UserRole role);

    List<InboxNotification> findByRecipientIdAndRecipientRoleOrderByOccurredAtDescIdDesc(
            Long recipientId, UserRole role, Pageable pageable);

    long countByRecipientIdAndRecipientRoleAndReadAtIsNull(Long recipientId, UserRole role);

    Optional<InboxNotification> findByIdAndRecipientIdAndRecipientRole(
            Long id, Long recipientId, UserRole role);
}
