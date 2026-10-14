package com.swiftapp_backend.driverservice.driver.api.controller;

import com.swiftapp_backend.driverservice.driver.api.dto.DriverStaticsDto;
import com.swiftapp_backend.driverservice.driver.application.service.DriverStaticsService;
import com.swiftapp_backend.driverservice.shared.common.ApiResponseFormat;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "DriverStatic Controller",description = "")
@RestController
@RequestMapping("/api/v1/driverStatic")
@RequiredArgsConstructor
public class DriverStaticController {
    private final DriverStaticsService driverStaticsService;

    @GetMapping("/getDriverStatic")
    public ResponseEntity<ApiResponseFormat<DriverStaticsDto>> getDriverStatic(@RequestHeader("x-user-id") Long authUserId, @RequestParam Long driverId) throws Exception {
        var res = driverStaticsService.getDriverStatics(authUserId);
        return ResponseEntity
                .ok()
                .body(ok(res,"Driver Static Fetched Successfully","/api/v1/driverStatic/getDriverStatic"));
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
