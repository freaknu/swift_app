package com.swiftapp_backend.locationservice.eta.api.controller;

import com.swiftapp_backend.locationservice.eta.api.dto.EtaRequest;
import com.swiftapp_backend.locationservice.eta.api.dto.EtaResponse;
import com.swiftapp_backend.locationservice.eta.application.port.in.CalculateEtaUseCase;
import com.swiftapp_backend.locationservice.eta.domain.model.Eta;
import com.swiftapp_backend.locationservice.shared.ApiResponseFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/location/eta")
@Tag(name = "ETA", description = "Traffic-aware route and arrival-time calculations")
public class EtaController {
    private final CalculateEtaUseCase calculateEtaUseCase;

    @PostMapping
    @Operation(summary = "Calculate an ETA from a driver's location to a destination")
    public ResponseEntity<ApiResponseFormat<EtaResponse>> calculateEta(
            @RequestHeader("x-user-id") Long authUserId,
            @Valid @RequestBody EtaRequest request,
            HttpServletRequest httpRequest) {
        Eta eta = calculateEtaUseCase.calculateEta(
                request.rideId(),
                request.driverId(),
                request.driverLatitude(),
                request.driverLongitude(),
                request.destinationLatitude(),
                request.destinationLongitude(),
                request.rideType()
        );

        EtaResponse response = new EtaResponse(
                eta.rideId(),
                eta.driverId(),
                eta.distanceKm(),
                eta.durationSeconds(),
                eta.etaMinutes(),
                eta.encodedPolyline()
        );

        return ResponseEntity.ok(ApiResponseFormat.<EtaResponse>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message("ETA calculated successfully")
                .endpoint(httpRequest.getRequestURI())
                .data(response)
                .build());
    }
}
