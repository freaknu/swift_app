package com.swiftapp_backend.noticationservice.notification.domain.model;

import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_inbox",
        uniqueConstraints = @UniqueConstraint(name = "inbox_event_recipient_unique",
                columnNames = {"event_key", "recipient_id", "recipient_role"}),
        indexes = @Index(name = "inbox_recipient_time_idx",
                columnList = "recipient_id,recipient_role,occurred_at"))
@Getter
@Setter
public class InboxNotification {
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

    private Long rideId;

    private Long sessionId;

    @Column(length = 128)
    private String messageId;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    @Column(nullable = false)
    private boolean pushDelivered;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (occurredAt == null) occurredAt = createdAt;
    }
}
