package com.swiftapp_backend.riderservice.ride.api.controller;

import com.swiftapp_backend.riderservice.ride.api.dto.ride.RideDto;
import com.swiftapp_backend.riderservice.ride.application.service.RideService;
import com.swiftapp_backend.riderservice.shared.ApiException;
import com.swiftapp_backend.riderservice.shared.ApiResponseFormat;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/rides")
public class InternalRideController {
    private final RideService rideService;

    @GetMapping("/{rideId}")
    public ApiResponseFormat<RideDto> getRide(@PathVariable Long rideId, HttpServletRequest request) {
        try {
            if (!InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()) {
                throw ApiException.forbidden("Internal lookup requires a local service");
            }
        } catch (UnknownHostException ex) {
            throw ApiException.forbidden("Internal lookup requires a local service");
        }
        return ApiResponseFormat.<RideDto>builder().success(true).statusCode(200)
                .message("Ride fetched").data(rideService.getRide(rideId))
                .endpoint(request.getRequestURI()).build();
    }
}
