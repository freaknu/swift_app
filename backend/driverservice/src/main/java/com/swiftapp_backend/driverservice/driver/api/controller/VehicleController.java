package com.swiftapp_backend.driverservice.driver.api.controller;

import com.swiftapp_backend.driverservice.driver.api.dto.VehicleCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.api.dto.VehicleResponseDto;
import com.swiftapp_backend.driverservice.driver.api.dto.VehicleTypeOptionDto;
import com.swiftapp_backend.driverservice.driver.application.service.VehicleService;
import com.swiftapp_backend.driverservice.driver.domain.enums.VehicleType;
import com.swiftapp_backend.driverservice.shared.common.ApiResponseFormat;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Arrays;
import java.util.Locale;

@Tag(
        name = "Vehicle Controller",
        description = "APIs for Vehicle Management"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/vehicle")
public class VehicleController {

    private static final String BASE_ENDPOINT = "/api/v1/vehicle";

    private final VehicleService vehicleService;

    @PostMapping
    @Operation(summary = "Add a new vehicle")
    public ResponseEntity<ApiResponseFormat<VehicleResponseDto>> addVehicle(
            @RequestHeader("x-user-id") Long userId,
            @Valid @RequestBody VehicleCreateRequestDto requestDto
    ) throws Exception {
        VehicleResponseDto response = vehicleService.addVehicle(userId, requestDto);
        return ResponseEntity.ok(
                ApiResponseFormat.<VehicleResponseDto>builder()
                        .success(true)
                        .message("Vehicle added successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT)
                        .data(response)
                        .build()
        );
    }

    @GetMapping("/types")
    @Operation(summary = "List supported vehicle types")
    public ResponseEntity<ApiResponseFormat<List<VehicleTypeOptionDto>>> getVehicleTypes() {
        List<VehicleTypeOptionDto> types = Arrays.stream(VehicleType.values())
                .map(type -> new VehicleTypeOptionDto(type.name(), displayName(type)))
                .toList();
        return ResponseEntity.ok(ApiResponseFormat.<List<VehicleTypeOptionDto>>builder()
                .success(true)
                .message("Vehicle types fetched successfully.")
                .statusCode(HttpStatus.OK.value())
                .timestamp(OffsetDateTime.now())
                .endpoint(BASE_ENDPOINT + "/types")
                .data(types)
                .build());
    }

    @PutMapping("/{vehicleId}")
    @Operation(summary = "Update an owned vehicle")
    public ResponseEntity<ApiResponseFormat<VehicleResponseDto>> updateVehicle(
            @RequestHeader("x-user-id") Long userId,
            @PathVariable Long vehicleId,
            @Valid @RequestBody VehicleCreateRequestDto requestDto) {
        VehicleResponseDto response = vehicleService.updateVehicle(userId, vehicleId, requestDto);
        return ResponseEntity.ok(ApiResponseFormat.<VehicleResponseDto>builder()
                .success(true)
                .message("Vehicle updated successfully.")
                .statusCode(HttpStatus.OK.value())
                .timestamp(OffsetDateTime.now())
                .endpoint(BASE_ENDPOINT + "/" + vehicleId)
                .data(response)
                .build());
    }

    private static String displayName(VehicleType type) {
        String[] words = type.name().toLowerCase(Locale.ROOT).split("_");
        for (int i = 0; i < words.length; i++) {
            words[i] = Character.toUpperCase(words[i].charAt(0)) + words[i].substring(1);
        }
        return String.join(" ", words);
    }

    @GetMapping
    public ResponseEntity<ApiResponseFormat<List<VehicleResponseDto>>> getAllVehicles(
            @RequestHeader("x-user-id") Long userId
    ) {

        List<VehicleResponseDto> response = vehicleService.getAllVehicle(userId);

        return ResponseEntity.ok(
                ApiResponseFormat.<List<VehicleResponseDto>>builder()
                        .success(true)
                        .message("Vehicles fetched successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT)
                        .data(response)
                        .build()
        );
    }

    @PutMapping("/verify")
    public ResponseEntity<ApiResponseFormat<Boolean>> verifyVehicle(
            @RequestHeader("x-user-id") Long userId,
            @RequestParam Long vehicleId,
            HttpServletRequest request
    ) throws Exception {
        InternalProfileController.requireLoopback(request);
        vehicleService.verifyVehicle(userId, vehicleId);

        return ResponseEntity.ok(
                ApiResponseFormat.<Boolean>builder()
                        .success(true)
                        .message("Vehicle verified successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/verify")
                        .data(true)
                        .build()
        );
    }

    @DeleteMapping("/deactivate")
    public ResponseEntity<ApiResponseFormat<Boolean>> deactivateVehicle(
            @RequestHeader("x-user-id") Long userId,
            @RequestParam Long vehicleId
    ) throws Exception {

        vehicleService.deactivateVehicle(userId, vehicleId);

        return ResponseEntity.ok(
                ApiResponseFormat.<Boolean>builder()
                        .success(true)
                        .message("Vehicle deactivated successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/deactivate")
                        .data(true)
                        .build()
        );
    }
}
