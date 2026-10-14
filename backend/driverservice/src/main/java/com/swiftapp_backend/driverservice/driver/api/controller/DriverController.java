package com.swiftapp_backend.driverservice.driver.api.controller;

import com.swiftapp_backend.driverservice.driver.api.dto.*;
import com.swiftapp_backend.driverservice.driver.domain.enums.DriverStatus;
import com.swiftapp_backend.driverservice.driver.application.service.DriverRegistrationService;
import com.swiftapp_backend.driverservice.driver.application.service.DriverOfferService;
import com.swiftapp_backend.driverservice.driver.application.service.DriverCacheService;
import com.swiftapp_backend.driverservice.shared.common.ApiResponseFormat;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@Tag(
        name = "Driver Controller",
        description = "APIs for Driver Registration, Profile, Status, Earnings, and Preferences Management"
)
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/driver")
public class DriverController {

    private static final String BASE_ENDPOINT = "/api/v1/driver";

    private final DriverRegistrationService driverRegistrationService;
    private final DriverCacheService driverCacheService;
    private final DriverOfferService driverOfferService;

    @GetMapping("/rides/active")
    public ResponseEntity<ApiResponseFormat<Long>> activeRide(
            @RequestHeader("x-user-id") Long userId) {
        return ResponseEntity.ok(ApiResponseFormat.<Long>builder()
                .success(true)
                .message("Active ride fetched.")
                .statusCode(HttpStatus.OK.value())
                .timestamp(OffsetDateTime.now())
                .endpoint(BASE_ENDPOINT + "/rides/active")
                .data(driverOfferService.getActiveRideId(userId))
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponseFormat<DriverResponseDto>> registerDriver(
            @RequestHeader("x-user-id") Long userId,
            @RequestBody DriverCreateRequestDto requestDto
    ) {

        DriverResponseDto response =
                driverRegistrationService.registerDriver(userId, requestDto);

        return ResponseEntity.ok(
                ApiResponseFormat.<DriverResponseDto>builder()
                        .success(true)
                        .message("Driver registered successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/register")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponseFormat<DriverResponseDto>> getDriverProfile(
            @RequestHeader("x-user-id") Long userId
    ) throws Exception {

        log.info("Getting driver detail");
        DriverResponseDto response =
                driverRegistrationService.getDriverProfile(userId);

        return ResponseEntity.ok(
                ApiResponseFormat.<DriverResponseDto>builder()
                        .success(true)
                        .message("Driver profile fetched successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/profile")
                        .data(response)
                        .build()
        );
    }


    @GetMapping("/profile/completeness")
    public ResponseEntity<ApiResponseFormat<ProfileCompletenessDto>> getProfileCompleteness(
            @RequestHeader("x-user-id") Long userId
    ) throws Exception {

        ProfileCompletenessDto response =
                driverRegistrationService.getProfileCompleteness(userId);

        return ResponseEntity.ok(
                ApiResponseFormat.<ProfileCompletenessDto>builder()
                        .success(true)
                        .message("Driver profile completeness fetched successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/profile/completeness")
                        .data(response)
                        .build()
        );
    }

    @PutMapping("/status")
    public ResponseEntity<ApiResponseFormat<Boolean>> updateDriverStatus(
            @RequestHeader("x-user-id") Long userId,
            @RequestParam DriverStatus driverStatus
    ) throws Exception {

        driverRegistrationService.changeStatus(userId, driverStatus);

        return ResponseEntity.ok(
                ApiResponseFormat.<Boolean>builder()
                        .success(true)
                        .message("Driver status updated successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/status")
                        .data(true)
                        .build()
        );
    }

    @GetMapping("/driverMiniDetail")
    public ResponseEntity<ApiResponseFormat<DriverSmallDetailDto>> getMiniDetail(
            @RequestHeader ("x-user-id") Long userId
    ) throws Exception {
        var res = driverCacheService.getMiniDriverData(userId);
        return ResponseEntity.ok(
                ApiResponseFormat.<DriverSmallDetailDto>builder()
                        .success(true)
                        .message("Driver mini detail fetched successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/driverMiniDetail")
                        .data(res.join())
                        .build());
    }

    @PutMapping("/suspend")
    @Operation(summary = "Suspend driver account")
    public ResponseEntity<ApiResponseFormat<Boolean>> suspendDriver(
            @RequestHeader("x-user-id") Long userId,
            @RequestBody DriverSuspensionRequestDto requestDto,
            HttpServletRequest request
    ) throws Exception {
        InternalProfileController.requireLoopback(request);
        driverRegistrationService.suspendDriver(userId, requestDto.getReason());
        return ResponseEntity.ok(
                ApiResponseFormat.<Boolean>builder()
                        .success(true)
                        .message("Driver suspended successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/suspend")
                        .data(true)
                        .build()
        );
    }

    @PutMapping("/reactivate")
    @Operation(summary = "Reactivate driver account")
    public ResponseEntity<ApiResponseFormat<Boolean>> reactivateDriver(
            @RequestHeader("x-user-id") Long userId,
            HttpServletRequest request
    ) throws Exception {
        InternalProfileController.requireLoopback(request);
        driverRegistrationService.reactivateDriver(userId);
        return ResponseEntity.ok(
                ApiResponseFormat.<Boolean>builder()
                        .success(true)
                        .message("Driver reactivated successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/reactivate")
                        .data(true)
                        .build()
        );
    }

    @GetMapping("/earnings")
    @Operation(summary = "Get driver earnings summary")
    public ResponseEntity<ApiResponseFormat<DriverEarningSummaryDto>> getEarnings(
            @RequestHeader("x-user-id") Long userId
    ) {
        DriverEarningSummaryDto response = driverRegistrationService.getEarnings(userId);
        return ResponseEntity.ok(
                ApiResponseFormat.<DriverEarningSummaryDto>builder()
                        .success(true)
                        .message("Driver earnings fetched successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/earnings")
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/notification-preferences")
    @Operation(summary = "Get driver notification preferences")
    public ResponseEntity<ApiResponseFormat<DriverNotificationPreferencesDto>> getNotificationPreferences(
            @RequestHeader("x-user-id") Long userId
    ) {
        DriverNotificationPreferencesDto response = driverRegistrationService.getNotificationPreferences(userId);
        return ResponseEntity.ok(
                ApiResponseFormat.<DriverNotificationPreferencesDto>builder()
                        .success(true)
                        .message("Driver notification preferences fetched successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/notification-preferences")
                        .data(response)
                        .build()
        );
    }

    @PutMapping("/notification-preferences")
    @Operation(summary = "Update driver notification preferences")
    public ResponseEntity<ApiResponseFormat<DriverNotificationPreferencesDto>> updateNotificationPreferences(
            @RequestHeader("x-user-id") Long userId,
            @RequestBody DriverNotificationPreferencesDto dto
    ) {
        DriverNotificationPreferencesDto response = driverRegistrationService.updateNotificationPreferences(userId, dto);
        return ResponseEntity.ok(
                ApiResponseFormat.<DriverNotificationPreferencesDto>builder()
                        .success(true)
                        .message("Driver notification preferences updated successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/notification-preferences")
                        .data(response)
                        .build()
        );
    }

    @PostMapping("/device-info")
    @Operation(summary = "Save driver device information")
    public ResponseEntity<ApiResponseFormat<Boolean>> saveDeviceInfo(
            @RequestHeader("x-user-id") Long userId,
            @RequestBody DriverDeviceInfoRequestDto dto
    ) {
        driverRegistrationService.saveDeviceInfo(userId, dto);
        return ResponseEntity.ok(
                ApiResponseFormat.<Boolean>builder()
                        .success(true)
                        .message("Driver device info saved successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/device-info")
                        .data(true)
                        .build()
        );
    }
}
