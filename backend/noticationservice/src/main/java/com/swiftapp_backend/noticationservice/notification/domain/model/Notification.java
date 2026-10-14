package com.swiftapp_backend.noticationservice.notification.domain.model;

import com.swiftapp_backend.noticationservice.notification.domain.enums.NotificationType;
import com.swiftapp_backend.noticationservice.notification.domain.enums.SenderType;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.AllArgsConstructor;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class Notification {
    private Long id;
    private String title;
    private String body;
    private String image;
    private NotificationType type;
    private Long senderId;
    private Long receiverId;
    private UserRole receiverRole;
    private SenderType senderType;
    private Object additionalData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
