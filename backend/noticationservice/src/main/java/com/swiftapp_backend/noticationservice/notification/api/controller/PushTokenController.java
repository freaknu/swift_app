package com.swiftapp_backend.noticationservice.notification.api.controller;

import com.swiftapp_backend.noticationservice.notification.application.service.PushTokenService;
import com.swiftapp_backend.noticationservice.notification.application.service.PushNotificationService;
import com.swiftapp_backend.noticationservice.notification.application.service.PushDeliveryResult;
import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.shared.ApiResponseFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notifications/devices")
public class PushTokenController {
    private final PushTokenService service;
    private final PushNotificationService push;
    public record Registration(String token, UserRole role, boolean enabled) {}
    public record DeviceStatus(boolean registered, boolean enabled, UserRole role, LocalDateTime updatedAt) {}
    public record TestResult(PushDeliveryResult deliveryStatus) {}

    @GetMapping("/{installationId}")
    public ResponseEntity<ApiResponseFormat<DeviceStatus>> status(@RequestHeader("x-user-id") Long userId,
                                                                 @PathVariable String installationId) {
        var device = service.ownedDevice(userId, installationId);
        return ResponseEntity.ok(ApiResponseFormat.<DeviceStatus>builder().success(true).statusCode(200)
                .message("Notification device status")
                .data(new DeviceStatus(true, device.isActive() && device.isNotificationEnabled(),
                        device.getRole(), device.getUpdatedAt())).build());
    }

    @PostMapping("/{installationId}/test")
    public ResponseEntity<ApiResponseFormat<TestResult>> test(@RequestHeader("x-user-id") Long userId,
                                                             @PathVariable String installationId) {
        var device = service.ownedDevice(userId, installationId);
        PushDeliveryResult result = !device.isActive() || !device.isNotificationEnabled()
                ? PushDeliveryResult.PREFERENCES_DISABLED
                : push.send(device.getUserId(), device.getRole(), "Swift notifications are connected",
                        "This test confirms that push delivery is connected to your account.",
                        Map.of("type", "PUSH_TEST"), 60);
        return ResponseEntity.ok(ApiResponseFormat.<TestResult>builder().success(true).statusCode(200)
                .message("Notification test processed").data(new TestResult(result)).build());
    }

    @PutMapping("/{installationId}")
    public ResponseEntity<ApiResponseFormat<Boolean>> register(@RequestHeader("x-user-id") Long userId,
                                                               @PathVariable String installationId, @RequestBody Registration request) {
        service.register(userId, installationId, request.token(), request.role(), request.enabled());
        return ResponseEntity.ok(ok());
    }

    @DeleteMapping("/{installationId}")
    public ResponseEntity<ApiResponseFormat<Boolean>> unregister(@RequestHeader("x-user-id") Long userId,
            @PathVariable String installationId) {
        service.unregister(userId, installationId);
        return ResponseEntity.ok(ok());
    }

    private ApiResponseFormat<Boolean> ok() {
        return ApiResponseFormat.<Boolean>builder().success(true).statusCode(200)
                .message("Notification device updated").data(true).build();
    }
}
