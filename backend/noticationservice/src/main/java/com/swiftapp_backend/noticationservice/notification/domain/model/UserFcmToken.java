package com.swiftapp_backend.noticationservice.notification.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
@Table(name = "user_fcm_tokens", indexes = @Index(name = "push_recipient", columnList = "user_id,role"))
public class UserFcmToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 64)
    private String installationId;
    @Column(nullable = false)
    private Long authUserId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private UserRole role;
    // driver or rider id
    private Long userId;
    @Column(nullable = false, unique = true, length = 1024)
    private String fcmToken;
    private boolean active;
    private boolean notificationEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
