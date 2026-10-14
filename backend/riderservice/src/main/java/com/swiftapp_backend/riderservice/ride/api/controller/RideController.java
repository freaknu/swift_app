package com.swiftapp_backend.riderservice.ride.api.controller;

import com.swiftapp_backend.riderservice.ride.api.dto.location.RecentLocationDto;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.*;
import com.swiftapp_backend.riderservice.ride.application.service.RideService;
import com.swiftapp_backend.riderservice.ride.application.service.RideIdentityService;
import com.swiftapp_backend.riderservice.ride.domain.enums.ChangedByType;
import com.swiftapp_backend.riderservice.shared.ApiException;
import com.swiftapp_backend.riderservice.shared.ApiResponseFormat;
import com.swiftapp_backend.riderservice.shared.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/ride")
@Tag(name = "Rides", description = "Rider booking and ride lifecycle operations")
public class RideController {
    private final RideService rideService;
    private final RideIdentityService identityService;

    @PostMapping("/requestRide")
    @Operation(summary = "Request a ride")
    public ResponseEntity<ApiResponseFormat<RideDto>> requestRide(
            @RequestHeader("x-user-id") Long authUserId,
            @Valid @RequestBody RideRequestDto request,
            HttpServletRequest httpRequest) {
        if (!authUserId.equals(request.authUserId())) {
            throw ApiException.forbidden("Ride request authUserId does not match authenticated user");
        }
        identityService.requireRider(authUserId, request.riderId());
        return success(rideService.requestRide(request),
                "Ride requested", httpRequest);
    }

    @GetMapping("/getRideDetail/{rideId}")
    @Operation(summary = "Get a ride")
    public ResponseEntity<ApiResponseFormat<RideDto>> getRide(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable @Positive Long rideId, HttpServletRequest request) {
        RideDto ride = rideService.getRide(rideId);
        identityService.requireParticipant(authUserId, ride.getRiderId(), ride.getDriverId());
        return success(ride, "Ride fetched", request);
    }

    @GetMapping("/getRideHistory/{rideId}")
    @Operation(summary = "Get ordered status history for a ride")
    public ResponseEntity<ApiResponseFormat<List<RideStatusHistoryDto>>> getRideHistory(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable @Positive Long rideId, HttpServletRequest request) {
        RideDto ride = rideService.getRide(rideId);
        identityService.requireParticipant(authUserId, ride.getRiderId(), ride.getDriverId());
        return success(rideService.getRideHistory(rideId),
                "Ride history fetched", request);
    }

    @GetMapping("/getRideHistoryPaginated/{riderId}")
    @Operation(summary = "Get paginated ride history for a rider")
    public ResponseEntity<ApiResponseFormat<PageResponse<RideDto>>> getRiderRides(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable @Positive Long riderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        identityService.requireRider(authUserId, riderId);
        if (page < 0 || size < 1 || size > 100) {
            throw com.swiftapp_backend.riderservice.shared.ApiException.badRequest(
                    "page must be >= 0 and size must be between 1 and 100");
        }
        var result = rideService.getRiderRides(riderId, page, size);
        return success(PageResponse.of(result.content(), result.page(), result.size(),
                        result.totalElements(), result.totalPages(), result.last()),
                "Rider rides fetched", request);
    }

    @GetMapping("/drivers/me/rides/history")
    @Operation(summary = "Get the authenticated driver's paginated ride history")
    public ResponseEntity<ApiResponseFormat<PageResponse<RideDto>>> getDriverRides(
            @RequestHeader("x-user-id") Long authUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus status,
            HttpServletRequest request) {
        if (page < 0 || size < 1 || size > 100) {
            throw ApiException.badRequest("page must be >= 0 and size must be between 1 and 100");
        }
        Long driverId = identityService.driverIdForAuthUser(authUserId);
        var result = rideService.getDriverRides(driverId, page, size, status);
        return success(PageResponse.of(result.content(), result.page(), result.size(),
                        result.totalElements(), result.totalPages(), result.last()),
                "Driver rides fetched", request);
    }

    @GetMapping("/riders/{riderId}/rides/active")
    @Operation(summary = "Get the rider's active ride")
    public ResponseEntity<ApiResponseFormat<RideDto>> getActiveRide(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable @Positive Long riderId, HttpServletRequest request) {
        identityService.requireRider(authUserId, riderId);
        return success(rideService.getActiveRide(riderId),
                "Active ride fetched", request);
    }

    @PostMapping("/rides/{rideId}/accept")
    @Operation(summary = "Assign a driver and vehicle to a requested ride")
    public ResponseEntity<ApiResponseFormat<RideDto>> acceptRide(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable @Positive Long rideId,
            @Valid @RequestBody AcceptRideRequest body,
            HttpServletRequest request) {
        throw ApiException.forbidden("Accept this ride through a targeted driver offer");
    }

    @PostMapping("/rides/{rideId}/arrive")
    @Operation(summary = "Mark the assigned driver as arrived")
    public ResponseEntity<ApiResponseFormat<RideDto>> driverArrived(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable @Positive Long rideId,
            @Valid @RequestBody RideActionRequest body,
            HttpServletRequest request) {
        identityService.requireDriver(authUserId, body.driverId());
        return success(rideService.driverArrived(rideId, body.driverId()),
                "Driver arrival recorded", request);
    }

    @PostMapping("/rides/{rideId}/start")
    @Operation(summary = "Start a ride after driver arrival")
    public ResponseEntity<ApiResponseFormat<RideDto>> startRide(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable @Positive Long rideId,
            @Valid @RequestBody RideActionRequest body,
            HttpServletRequest request) {
        identityService.requireDriver(authUserId, body.driverId());
        return success(rideService.startRide(rideId, body.driverId()),
                "Ride started", request);
    }

    @PostMapping("/rides/{rideId}/complete")
    @Operation(summary = "Complete an active ride")
    public ResponseEntity<ApiResponseFormat<RideDto>> completeRide(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable @Positive Long rideId,
            @Valid @RequestBody RideActionRequest body,
            HttpServletRequest request) {
        identityService.requireDriver(authUserId, body.driverId());
        return success(rideService.completeRide(rideId, body.driverId()),
                "Ride completed", request);
    }

    @PostMapping("/rides/{rideId}/cancel")
    @Operation(summary = "Cancel a ride before it starts")
    public ResponseEntity<ApiResponseFormat<RideDto>> cancelRide(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable @Positive Long rideId,
            @Valid @RequestBody CancelRideRequest body,
            HttpServletRequest request) {
        if (body.actorType() == ChangedByType.RIDER) {
            identityService.requireRider(authUserId, body.actorId());
        } else if (body.actorType() == ChangedByType.DRIVER) {
            identityService.requireDriver(authUserId, body.actorId());
        } else {
            throw ApiException.forbidden("Only the assigned rider or driver can cancel through this endpoint");
        }
        return success(rideService.cancelRide(rideId, body),
                "Ride cancelled", request);
    }

    @PostMapping("/rides/getRideLocationSuggestion/{riderId}")
    @Operation(summary = "Get the ride suggestion")
    public ResponseEntity<ApiResponseFormat<List<RecentLocationDto>>> getAllLocationSuggestion(@RequestHeader("x-user-id") Long authUserId,@PathVariable Long riderId,@RequestParam double latitude,@RequestParam double longitude,HttpServletRequest request) {
        identityService.requireRider(authUserId, riderId);
        return success(rideService.getRideLocationSuggestions(riderId,latitude,longitude), "Suggestion Fetched Successfully",request);
    }

    private <T> ResponseEntity<ApiResponseFormat<T>> success(
            T data, String message, HttpServletRequest request) {
        ApiResponseFormat<T> response = ApiResponseFormat.<T>builder()
                .success(true).statusCode(HttpStatus.OK.value()).message(message)
                .endpoint(request.getRequestURI()).data(data).build();
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
