package com.swiftapp_backend.riderservice.ride.api.controller;

import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareDto;
import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareEstimateDto;
import com.swiftapp_backend.riderservice.ride.api.dto.fare.FareEstimateRequest;
import com.swiftapp_backend.riderservice.ride.application.service.FareService;
import com.swiftapp_backend.riderservice.ride.application.service.RideIdentityService;
import com.swiftapp_backend.riderservice.ride.application.service.RideService;
import com.swiftapp_backend.riderservice.shared.ApiResponseFormat;
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

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/fare")
@Tag(name = "Fares", description = "Fare estimation and persisted ride fares")
public class FareController {
    private final FareService fareService;
    private final RideService rideService;
    private final RideIdentityService identityService;

    @PostMapping("/estimate")
    @Operation(summary = "Estimate a fare before requesting a ride")
    public ResponseEntity<ApiResponseFormat<FareEstimateDto>> estimateFare(@RequestHeader("x-user-id") Long authUserId,
            @Valid @RequestBody FareEstimateRequest request,
            HttpServletRequest httpRequest) {
        return ok(fareService.estimate(request), "Fare estimated", httpRequest);
    }

    @GetMapping("/rides/{rideId}/fare")
    @Operation(summary = "Get the fare attached to a ride")
    public ResponseEntity<ApiResponseFormat<FareDto>> getRideFare(@RequestHeader("x-user-id") Long authUserId,
            @PathVariable @Positive Long rideId,
            HttpServletRequest request) {
        var ride = rideService.getRide(rideId);
        identityService.requireParticipant(authUserId, ride.getRiderId(), ride.getDriverId());
        return ok(fareService.getFareByRideId(rideId), "Fare fetched", request);
    }


    private <T> ResponseEntity<ApiResponseFormat<T>> ok(T data, String message,
                                                        HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponseFormat.<T>builder()
                .success(true).statusCode(HttpStatus.OK.value()).data(data)
                .message(message).endpoint(request.getRequestURI()).build());
    }
}
