package com.swiftapp_backend.analyticalservice.analytics.api.controller;

import com.swiftapp_backend.analyticalservice.analytics.api.dto.DriverAnalyticsResponseDto;
import com.swiftapp_backend.analyticalservice.analytics.api.dto.RiderAnalyticsResponseDto;
import com.swiftapp_backend.analyticalservice.analytics.api.dto.DashboardMetricsDto;
import com.swiftapp_backend.analyticalservice.analytics.application.service.AnalyticsQueryService;
import com.swiftapp_backend.analyticalservice.analytics.application.port.AnalyticsProfilePort;
import com.swiftapp_backend.analyticalservice.shared.ApiResponseFormat;
import com.swiftapp_backend.analyticalservice.shared.AnalyticsAccessDeniedException;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Analytics Controller", description = "Analytics Controller for Analytics Service")
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {
    private final AnalyticsQueryService analyticsQueryService;
    private final AnalyticsProfilePort profiles;

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponseFormat<DriverAnalyticsResponseDto>> getDriverAnalytics(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable Long driverId) {
        requireOwner(authUserId, profiles.driverAuthUserId(driverId));
        var analytics = analyticsQueryService.getDriverAnalytics(driverId);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<DriverAnalyticsResponseDto>builder()
                        .data(analytics)
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Driver analytics fetched successfully")
                        .errors(null)
                        .endpoint("/api/v1/analytics/driver/" + driverId)
                        .build());
    }

    @GetMapping("/rider/{riderId}")
    public ResponseEntity<ApiResponseFormat<RiderAnalyticsResponseDto>> getRiderAnalytics(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable Long riderId) {
        requireOwner(authUserId, profiles.riderAuthUserId(riderId));
        var analytics = analyticsQueryService.getRiderAnalytics(riderId);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<RiderAnalyticsResponseDto>builder()
                        .data(analytics)
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Rider analytics fetched successfully")
                        .errors(null)
                        .endpoint("/api/v1/analytics/rider/" + riderId)
                        .build());
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponseFormat<DashboardMetricsDto>> getDashboardMetrics(
            @RequestHeader("x-user-id") Long authUserId) {
        // JWTs currently contain only an account ID and no trusted admin role.
        throw new AnalyticsAccessDeniedException("Dashboard analytics require administrator authorization");
    }

    @GetMapping("/driver/{driverId}/earnings")
    public ResponseEntity<ApiResponseFormat<Double>> getDriverEarnings(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable Long driverId,
            @RequestParam(required = false) String period) {
        requireOwner(authUserId, profiles.driverAuthUserId(driverId));
        var earnings = analyticsQueryService.getDriverEarnings(driverId, period);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<Double>builder()
                        .data(earnings)
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Driver earnings fetched successfully")
                        .errors(null)
                        .endpoint("/api/v1/analytics/driver/" + driverId + "/earnings")
                        .build());
    }

    private void requireOwner(Long authUserId, Long profileAuthUserId) {
        if (authUserId == null || !authUserId.equals(profileAuthUserId)) {
            throw new AnalyticsAccessDeniedException("Analytics do not belong to the authenticated user");
        }
    }
}
