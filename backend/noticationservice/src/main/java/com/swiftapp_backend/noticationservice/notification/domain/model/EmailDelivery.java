package com.swiftapp_backend.noticationservice.notification.domain.model;

import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_email_deliveries",
        uniqueConstraints = @UniqueConstraint(name = "email_event_recipient_unique",
                columnNames = {"event_key", "recipient_id", "recipient_role"}))
@Getter
@Setter
public class EmailDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, length = 192)
    private String eventKey;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_role", nullable = false, length = 16)
    private UserRole recipientRole;

    @Column(nullable = false)
    private boolean sent;

    private LocalDateTime sentAt;
}
