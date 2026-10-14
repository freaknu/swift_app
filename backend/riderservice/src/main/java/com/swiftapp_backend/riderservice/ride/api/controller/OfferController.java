package com.swiftapp_backend.riderservice.ride.api.controller;

import com.swiftapp_backend.riderservice.ride.api.dto.offer.OfferFetchRequestDto;
import com.swiftapp_backend.riderservice.ride.api.dto.offer.OfferResponseDto;
import com.swiftapp_backend.riderservice.ride.application.service.OfferService;
import com.swiftapp_backend.riderservice.shared.ApiResponseFormat;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/offer")
@RequiredArgsConstructor
public class OfferController {
    private final OfferService offerService;

    @PostMapping("/getOffers")
    @Operation(description = "Api for Fetch the offers")
    public ResponseEntity<ApiResponseFormat<List<OfferResponseDto>>> getAllOffers(@RequestHeader("x-user-id") Long authUserId, @RequestBody OfferFetchRequestDto data) {
        var res = offerService.getAllOffers(authUserId,data);
        return ResponseEntity
                .ok()
                .body(ApiResponseFormat.<List<OfferResponseDto>>builder()
                        .statusCode(HttpStatus.OK.value())
                        .data(res)
                        .message("Offer Fetched Successfully")
                        .timestamp(OffsetDateTime.now())
                        .success(true)
                        .errors(null)
                        .endpoint("/api/v1/offer/getOffers")
                        .build());
    }
}
