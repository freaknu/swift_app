package com.swiftapp_backend.riderservice.ride.api.controller;

import com.swiftapp_backend.riderservice.ride.api.dto.ride.AssignedDriverSummaryDto;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideDto;
import com.swiftapp_backend.riderservice.ride.application.port.DriverRideSummaryPort;
import com.swiftapp_backend.riderservice.ride.application.service.RideIdentityService;
import com.swiftapp_backend.riderservice.ride.application.service.RideService;
import com.swiftapp_backend.riderservice.shared.ApiResponseFormat;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ride/rides")
public class RideDriverSummaryController {
    private final RideService rides;
    private final RideIdentityService identities;
    private final DriverRideSummaryPort summaries;

    @GetMapping("/{rideId}/driver-summary")
    public ApiResponseFormat<AssignedDriverSummaryDto> get(
            @RequestHeader("x-user-id") Long authUserId, @PathVariable Long rideId,
            HttpServletRequest request) {
        RideDto ride = rides.getRide(rideId);
        identities.requireRider(authUserId, ride.getRiderId());
        AssignedDriverSummaryDto summary = ride.getDriverId() == null || ride.getVehicleId() == null
                ? null : summaries.get(ride.getDriverId(), ride.getVehicleId());
        return ApiResponseFormat.<AssignedDriverSummaryDto>builder()
                .success(true).statusCode(200).message("Assigned driver fetched")
                .data(summary).endpoint(request.getRequestURI()).build();
    }
}
