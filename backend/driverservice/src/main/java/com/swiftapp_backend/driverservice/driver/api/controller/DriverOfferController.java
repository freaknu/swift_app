package com.swiftapp_backend.driverservice.driver.api.controller;

import com.swiftapp_backend.driverservice.driver.api.dto.PendingOfferDto;
import com.swiftapp_backend.driverservice.driver.application.service.DriverOfferService;
import com.swiftapp_backend.driverservice.driver.application.service.OfferExpiredException;
import com.swiftapp_backend.driverservice.driver.application.service.OfferNotFoundException;
import com.swiftapp_backend.driverservice.driver.application.service.MatchingDecisionUnavailableException;
import com.swiftapp_backend.driverservice.shared.common.ApiResponseFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.Instant;
import java.util.List;

@Tag(
        name = "Driver Offer Controller",
        description = "Ride offer inbox: view pending offers, accept or reject them. " +
                "Matching confirms each decision before the API returns success."
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/driver/offers")
public class DriverOfferController {

    private static final String BASE_ENDPOINT = "/api/v1/driver/offers";

    private final DriverOfferService offerService;

    @GetMapping("/active-ride")
    public ResponseEntity<ApiResponseFormat<Long>> activeRide(
            @RequestHeader("x-user-id") Long userId) {
        Long rideId = offerService.getActiveRideId(userId);
        return ResponseEntity.ok(ApiResponseFormat.<Long>builder()
                .success(true).message("Active ride fetched.")
                .statusCode(HttpStatus.OK.value())
                .timestamp(OffsetDateTime.now())
                .endpoint(BASE_ENDPOINT + "/active-ride")
                .data(rideId).build());
    }

    @Operation(summary = "List ride offers waiting for this driver's decision")
    @GetMapping
    public ResponseEntity<ApiResponseFormat<List<PendingOfferDto>>> pendingOffers(
            @RequestHeader("x-user-id") Long userId
    ) {
        final List<PendingOfferDto> offers;
        try {
            offers = offerService.getPendingOffers(userId);
        } catch (OfferNotFoundException e) {
            return failure(HttpStatus.NOT_FOUND, e.getMessage(), BASE_ENDPOINT);
        }
        String serverTime = Instant.now().toString();
        offers.forEach(offer -> offer.setServerTime(serverTime));
        return ResponseEntity.ok(
                ApiResponseFormat.<List<PendingOfferDto>>builder()
                        .success(true)
                        .message("Pending offers fetched successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT)
                        .data(offers)
                        .build()
        );
    }

    @Operation(summary = "Accept a ride offer after matching confirms it")
    @PostMapping("/{rideId}/accept")
    public ResponseEntity<ApiResponseFormat<Boolean>> acceptOffer(
            @RequestHeader("x-user-id") Long userId,
            @PathVariable Long rideId
    ) {
        try {
            offerService.acceptOffer(userId, rideId);
        } catch (OfferNotFoundException e) {
            return failure(HttpStatus.NOT_FOUND, e.getMessage(),
                    BASE_ENDPOINT + "/" + rideId + "/accept");
        } catch (OfferExpiredException e) {
            return failure(HttpStatus.GONE, e.getMessage(),
                    BASE_ENDPOINT + "/" + rideId + "/accept");
        } catch (MatchingDecisionUnavailableException e) {
            return failure(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(),
                    BASE_ENDPOINT + "/" + rideId + "/accept");
        }
        return ResponseEntity.ok(
                ApiResponseFormat.<Boolean>builder()
                        .success(true)
                        .message("Offer accepted successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/" + rideId + "/accept")
                        .data(true)
                        .build()
        );
    }

    @Operation(summary = "Reject a ride offer and advance matching")
    @PostMapping("/{rideId}/reject")
    public ResponseEntity<ApiResponseFormat<Boolean>> rejectOffer(
            @RequestHeader("x-user-id") Long userId,
            @PathVariable Long rideId,
            @RequestParam(required = false) String reason
    ) {
        try {
            offerService.rejectOffer(userId, rideId, reason);
        } catch (OfferNotFoundException e) {
            return failure(HttpStatus.NOT_FOUND, e.getMessage(),
                    BASE_ENDPOINT + "/" + rideId + "/reject");
        } catch (OfferExpiredException e) {
            return failure(HttpStatus.GONE, e.getMessage(),
                    BASE_ENDPOINT + "/" + rideId + "/reject");
        } catch (MatchingDecisionUnavailableException e) {
            return failure(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(),
                    BASE_ENDPOINT + "/" + rideId + "/reject");
        }
        return ResponseEntity.ok(
                ApiResponseFormat.<Boolean>builder()
                        .success(true)
                        .message("Offer rejected successfully.")
                        .statusCode(HttpStatus.OK.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(BASE_ENDPOINT + "/" + rideId + "/reject")
                        .data(true)
                        .build()
        );
    }

    private <T> ResponseEntity<ApiResponseFormat<T>> failure(
            HttpStatus status, String message, String endpoint) {
        return ResponseEntity.status(status).body(
                ApiResponseFormat.<T>builder()
                        .success(false)
                        .message(message)
                        .statusCode(status.value())
                        .timestamp(OffsetDateTime.now())
                        .endpoint(endpoint)
                        .data(null)
                        .build()
        );
    }
}
