package com.swiftapp_backend.riderservice.ride.api.controller;

import com.swiftapp_backend.riderservice.ride.api.dto.ride.AssignedRiderSummaryDto;
import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideDto;
import com.swiftapp_backend.riderservice.ride.application.port.RiderRideSummaryPort;
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
public class RideRiderSummaryController {
    private final RideService rides;
    private final RideIdentityService identities;
    private final RiderRideSummaryPort summaries;

    @GetMapping("/{rideId}/rider-summary")
    public ApiResponseFormat<AssignedRiderSummaryDto> get(
            @RequestHeader("x-user-id") Long authUserId, @PathVariable Long rideId,
            HttpServletRequest request) {
        RideDto ride = rides.getRide(rideId);
        identities.requireDriver(authUserId, ride.getDriverId());
        return ApiResponseFormat.<AssignedRiderSummaryDto>builder()
                .success(true).statusCode(200).message("Assigned rider fetched")
                .data(summaries.get(ride.getRiderId()))
                .endpoint(request.getRequestURI()).build();
    }
}
