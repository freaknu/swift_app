package com.swiftapp_backend.riderservice.ride.api.controller;

import com.swiftapp_backend.riderservice.ride.application.service.RideService;
import com.swiftapp_backend.riderservice.shared.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

/** Direct service-to-service lookup; the API gateway does not route /internal. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/rides")
public class InternalDriverRideController {
    private final RideService rideService;

    @GetMapping("/drivers/{driverId}/active-id")
    public ResponseEntity<Long> activeDriverRideId(@PathVariable Long driverId,
                                                   HttpServletRequest request) {
        try {
            if (!InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()) {
                throw ApiException.forbidden("Internal lookup requires a local service");
            }
        } catch (UnknownHostException ex) {
            throw ApiException.forbidden("Internal lookup requires a local service");
        }
        return ResponseEntity.ok(rideService.getActiveDriverRideId(driverId));
    }
}
