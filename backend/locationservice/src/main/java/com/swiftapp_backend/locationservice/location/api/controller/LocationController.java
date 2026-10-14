package com.swiftapp_backend.locationservice.location.api.controller;

import com.swiftapp_backend.locationservice.location.api.dto.driver.NearbyDriverDto;
import com.swiftapp_backend.locationservice.location.api.dto.event.LocationUpdateDto;
import com.swiftapp_backend.locationservice.location.application.service.LocationService;
import com.swiftapp_backend.locationservice.shared.ApiResponseFormat;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Live Location Controller",description = "Live Location Controller for Location Service")
@Slf4j
@RestController
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {
    private final LocationService locationService;

    @PostMapping("/saveLocation")
    public ResponseEntity<ApiResponseFormat<List<LocationUpdateDto>>> saveLocation(@RequestHeader("x-user-id") Long authUserId,@RequestBody LocationUpdateDto data) {
        var res = locationService.addDriverLocation(data, authUserId);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<List<LocationUpdateDto>>builder()
                        .data(res)
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Location Saved Successfully .")
                        .errors(null)
                        .endpoint("/api/location/saveLocation")
                        .build());
    }

    @GetMapping("/getRecentLocation/{rideId}/{driverId}")
    public ResponseEntity<ApiResponseFormat<List<LocationUpdateDto>>> getRecentLocation(@RequestHeader("x-user-id") Long authUserId,@PathVariable Long rideId,
                                                                                        @PathVariable Long driverId) {
        locationService.requireRideParticipant(rideId, driverId, authUserId);
        var res = locationService.getRecentLocation(rideId,driverId);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<List<LocationUpdateDto>>builder()
                        .data(res)
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Recent Location Fetched Successfully .")
                        .errors(null)
                        .endpoint("/api/location/getRecentLocation/"+rideId+"/"+driverId)
                        .build());
    }

    @GetMapping("/getLocationHistory/{rideId}/{driverId}")
    public ResponseEntity<ApiResponseFormat<List<LocationUpdateDto>>> getLocationHistory(@RequestHeader("x-user-id") Long authUserId,@PathVariable Long rideId,
                                                                                        @PathVariable Long driverId) {
        locationService.requireRideParticipant(rideId, driverId, authUserId);
        var res = locationService.getLocationHistory(rideId,driverId);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<List<LocationUpdateDto>>builder()
                        .data(res)
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Location History Fetched Successfully .")
                        .errors(null)
                        .endpoint("/api/location/getLocationHistory/"+rideId+"/"+driverId)
                        .build());
    }

    @PostMapping("/completeRide")
    public ResponseEntity<ApiResponseFormat<?>> completeRide(@RequestHeader("x-user-id") Long authUserId,@RequestParam Long rideId) {
        locationService.completeRide(rideId, authUserId);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<List<LocationUpdateDto>>builder()
                        .data(null)
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Ride Completed Successfully .")
                        .errors(null)
                        .endpoint("/api/location/completeRide")
                        .build());
    }

    @GetMapping("/nearbyDrivers")
    public ResponseEntity<ApiResponseFormat<List<NearbyDriverDto>>> getNearbyDrivers(
            @RequestHeader("x-user-id") Long authUserId,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam double distance,
            @RequestParam(required = false) String vehicleType
    ) {

        var res = locationService.getAllDrivers(latitude, longitude, distance, vehicleType);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<List<NearbyDriverDto>>builder()
                        .data(res)
                        .success(true)
                        .statusCode(HttpStatus.OK.value())
                        .message("Nearby Drivers Fetched Successfully .")
                        .errors(null)
                        .endpoint("/api/location/nearbyDrivers")
                        .build());
    }

    @GetMapping("/rides/{rideId}/driver-position")
    public ResponseEntity<ApiResponseFormat<LocationUpdateDto>> driverPosition(
            @RequestHeader("x-user-id") Long authUserId, @PathVariable Long rideId) {
        LocationUpdateDto position = locationService.getDriverPosition(rideId, authUserId);
        return ResponseEntity.ok(ApiResponseFormat.<LocationUpdateDto>builder()
                .data(position).success(true).statusCode(HttpStatus.OK.value())
                .message("Driver position fetched")
                .endpoint("/api/v1/location/rides/" + rideId + "/driver-position")
                .build());
    }
}
