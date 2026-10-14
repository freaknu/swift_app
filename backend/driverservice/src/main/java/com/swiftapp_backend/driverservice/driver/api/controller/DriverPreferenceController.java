package com.swiftapp_backend.driverservice.driver.api.controller;

import com.swiftapp_backend.driverservice.driver.api.dto.DriverPreferenceCreateRequestDto;
import com.swiftapp_backend.driverservice.driver.api.dto.DriverPreferenceResponseDto;
import com.swiftapp_backend.driverservice.driver.application.service.DriverPreferenceService;
import com.swiftapp_backend.driverservice.shared.common.ApiResponseFormat;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Driver Preference Controller",description = "Driver Preference Controller for Driver Service")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/driverPreference")
public class DriverPreferenceController {
    private final DriverPreferenceService driverPreferenceService;

    @GetMapping("/getDriverPreference/{driverId}")
    public ResponseEntity<ApiResponseFormat<DriverPreferenceResponseDto>> getDriverPreference(@RequestHeader("x-user-id") Long authUserId,@PathVariable Long driverId) throws Exception {
        var res = driverPreferenceService.getDriverPreference(driverId);
        return ResponseEntity
                .ok()
                .body(ok(res,"Driver Preference Fetched Successfully","/api/v1/driverPreference/getDriverPreference/" + driverId));
    }

    @PostMapping("/addDriverPreference")
    public ResponseEntity<ApiResponseFormat<DriverPreferenceResponseDto>> addDriverPreference(@RequestHeader("x-user-id") Long authUserId,@RequestBody DriverPreferenceCreateRequestDto data) throws Exception {
        var res = driverPreferenceService.addDriverPreference(data);
        return ResponseEntity
                .ok()
                .body(ok(res,"Driver Preference Saved Successfully","/api/v1/driverPreference/addDriverPreference"));
    }

    @PutMapping("/updateDriverPreference")
    public ResponseEntity<ApiResponseFormat<DriverPreferenceResponseDto>> updateDriverPreference(@RequestHeader("x-user-id") Long authUserId,@RequestBody DriverPreferenceCreateRequestDto data) throws Exception {
        var res = driverPreferenceService.updateDriverPreference(data);
        return ResponseEntity
                .ok()
                .body(ok(res,"Driver Preference Updated Successfully","/api/v1/driverPreference/updateDriverPreference"));
    }

    private <T> ApiResponseFormat<T> ok(T data, String message, String endpoint) {
        return ApiResponseFormat.<T>builder()
                .success(true)
                .statusCode(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .endpoint(endpoint)
                .errors(null)
                .build();
    }
}
