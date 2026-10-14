package com.swiftapp_backend.ratingservice.rating.api.controller;


import com.swiftapp_backend.ratingservice.rating.api.dto.RateUserCreateRequestDto;
import com.swiftapp_backend.ratingservice.rating.api.dto.RatingCreateResponseDto;
import com.swiftapp_backend.ratingservice.rating.api.dto.RateUserCreateResponseDto;
import com.swiftapp_backend.ratingservice.rating.application.service.RateUserService;
import com.swiftapp_backend.ratingservice.rating.application.service.RatingService;
import com.swiftapp_backend.ratingservice.shared.ApiResponseFormat;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "RateUser Controller",description = "Rating User Controller for Rating Service")
@RequestMapping("/api/v1/rating")
@RestController
@RequiredArgsConstructor
public class RateUserController {
    private final RateUserService rateUserService;
    private final RatingService ratingService;

    @GetMapping("/rides/{rideId}/mine")
    public ResponseEntity<ApiResponseFormat<RateUserCreateResponseDto>> getMyRideRating(
            @RequestHeader("x-user-id") Long authUserId, @PathVariable Long rideId) {
        return ResponseEntity.ok(ApiResponseFormat.<RateUserCreateResponseDto>builder()
                .data(ratingService.getMyRideRating(authUserId, rideId))
                .success(true).statusCode(HttpStatus.OK.value())
                .message("Ride rating fetched")
                .endpoint("/api/v1/rating/rides/" + rideId + "/mine").build());
    }

    @GetMapping("/getRating")
    public ResponseEntity<ApiResponseFormat<RatingCreateResponseDto>> getRating(@RequestHeader("x-user-id") Long authUserId,
            @RequestParam(required = false) Long driverId, @RequestParam(required = false) Long riderId) {
        var res = ratingService.getRating(authUserId, driverId, riderId);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<RatingCreateResponseDto>builder()
                        .data(res)
                        .success(true)
                        .message("Rating Fetched Successfully")
                        .statusCode(HttpStatus.OK.value())
                        .endpoint("/api/rating/getRating")
                        .build());
    }

    @PostMapping("/createRating")
    public ResponseEntity<ApiResponseFormat<RatingCreateResponseDto>> addRating(@RequestHeader("x-user-id") Long authUserId,
            @Valid @RequestBody RateUserCreateRequestDto data, @RequestParam(required = false) Long ratingId) {
        var res = ratingService.addRating(authUserId, data, ratingId);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<RatingCreateResponseDto>builder()
                        .data(res)
                        .success(true)
                        .message("Rating Added Successfully")
                        .statusCode(HttpStatus.OK.value())
                        .endpoint("/api/rating/createRating")
                        .build());
    }

    @DeleteMapping("/deleteRating/{rateId}")
    public ResponseEntity<ApiResponseFormat<?>> deleteRating(@RequestHeader("x-user-id") Long authUserId,@PathVariable Long rateId) {
        rateUserService.deleteRating(authUserId, rateId);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<RatingCreateResponseDto>builder()
                        .data(null)
                        .success(true)
                        .message("Rating Deleted Successfully")
                        .statusCode(HttpStatus.OK.value())
                        .endpoint("/api/rating/deleteRating/"+rateId)
                        .build());
    }
}
