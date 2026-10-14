package com.swiftapp_backend.riderservice.ride.api.controller;

import com.swiftapp_backend.riderservice.ride.domain.model.RideTypesModel;
import com.swiftapp_backend.riderservice.ride.application.service.RideTypesService;
import com.swiftapp_backend.riderservice.shared.ApiResponseFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "RideType Controller",description = "RideType Controller for Rider Service")
@RestController
@RequestMapping("/api/v1/ridetype")
@RequiredArgsConstructor
public class RideTypesController {
    private final RideTypesService rideTypesService;

    @GetMapping("/getAllRideTypes")
    @Operation(summary = "Get the ride types")
    public ResponseEntity<ApiResponseFormat<List<RideTypesModel>>> getAllRideTypes(@RequestHeader("x-user-id") Long authUserId,
                                                                                   @RequestParam Boolean isDriver,
                                                                                   HttpServletRequest request) {
        return success(rideTypesService.getRideTypes(authUserId,isDriver),"Ride Types Fetched Successfully",request);
    }

    private <T> ResponseEntity<ApiResponseFormat<T>> success(
            T data, String message, HttpServletRequest request) {
        ApiResponseFormat<T> response = ApiResponseFormat.<T>builder()
                .success(true).statusCode(HttpStatus.OK.value()).message(message)
                .endpoint(request.getRequestURI()).data(data).build();
        return ResponseEntity.status(HttpStatus.OK.value()).body(response);
    }
}
