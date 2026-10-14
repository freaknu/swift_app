package com.swiftapp_backend.noticationservice.notification.api.controller;

import com.swiftapp_backend.noticationservice.notification.api.dto.InboxNotificationDto;
import com.swiftapp_backend.noticationservice.notification.application.service.NotificationInboxService;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.shared.ApiResponseFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications")
public class NotificationInboxController {
    private final NotificationInboxService service;

    @GetMapping
    public ResponseEntity<ApiResponseFormat<List<InboxNotificationDto>>> list(
            @RequestHeader("x-user-id") Long authUserId,
            @RequestParam UserRole role,
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(ok(service.list(authUserId, role, limit), "Notifications fetched"));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponseFormat<Long>> unreadCount(
            @RequestHeader("x-user-id") Long authUserId,
            @RequestParam UserRole role) {
        return ResponseEntity.ok(ok(service.unreadCount(authUserId, role), "Unread count fetched"));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponseFormat<InboxNotificationDto>> markRead(
            @RequestHeader("x-user-id") Long authUserId,
            @RequestParam UserRole role,
            @PathVariable Long notificationId) {
        return ResponseEntity.ok(ok(service.markRead(authUserId, role, notificationId),
                "Notification marked read"));
    }

    private static <T> ApiResponseFormat<T> ok(T value, String message) {
        return ApiResponseFormat.<T>builder().success(true).statusCode(200)
                .message(message).data(value).build();
    }
}
