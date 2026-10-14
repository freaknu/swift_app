package com.swiftapp_backend.matchingservice.matching.api.controller;

import com.swiftapp_backend.matchingservice.matching.application.port.MatchingModuleApi;
import com.swiftapp_backend.matchingservice.matching.application.service.OfferResolution;
import com.swiftapp_backend.matchingservice.matching.domain.models.MatchingState;
import com.swiftapp_backend.matchingservice.matching.domain.repository.DriverOfferRepository;
import com.swiftapp_backend.matchingservice.shared.ApiResponseFormat;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Matching Controller")
@RestController
@RequestMapping("/api/v1/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingModuleApi matchingModuleApi;

    @PostMapping("/offers/{rideId}/accept")
    public ResponseEntity<ApiResponseFormat<OfferResolution>> acceptOffer(
            @PathVariable Long rideId,
            @RequestParam Long driverId,
            @RequestParam(required = false) Long vehicleId) {
        OfferResolution resolution = matchingModuleApi.acceptOffer(rideId, driverId, vehicleId);
        return ResponseEntity.ok(response(resolution,
                "Offer accept processed: " + resolution,
                "/api/v1/matching/offers/" + rideId + "/accept"));
    }

    @PostMapping("/offers/{rideId}/reject")
    public ResponseEntity<ApiResponseFormat<OfferResolution>> rejectOffer(
            @PathVariable Long rideId,
            @RequestParam Long driverId,
            @RequestParam(required = false) String reason) {
        OfferResolution resolution = matchingModuleApi.rejectOffer(rideId, driverId, reason);
        return ResponseEntity.ok(response(resolution,
                "Offer reject processed: " + resolution,
                "/api/v1/matching/offers/" + rideId + "/reject"));
    }

    /** Rider service calls this before committing a pending-ride cancellation. */
    @PostMapping("/rides/{rideId}/cancel-pending")
    public ResponseEntity<ApiResponseFormat<DriverOfferRepository.CancelOutcome>> cancelPendingRide(
            @PathVariable Long rideId) {
        DriverOfferRepository.CancelOutcome outcome = matchingModuleApi.cancelPendingRide(rideId);
        return ResponseEntity.ok(response(outcome,
                "Pending ride cancellation processed: " + outcome,
                "/api/v1/matching/rides/" + rideId + "/cancel-pending"));
    }


    @GetMapping("/rides/{rideId}")
    public ResponseEntity<ApiResponseFormat<MatchingState>> matchingState(
            @PathVariable Long rideId) {
        return matchingModuleApi.matchingState(rideId)
                .map(state -> ResponseEntity.ok(response(state,
                        "Matching state fetched.", "/api/v1/matching/rides/" + rideId)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(response(null, "No matching state for ride " + rideId,
                                "/api/v1/matching/rides/" + rideId)));
    }

    private <T> ApiResponseFormat<T> response(T data, String message, String endpoint) {
        return ApiResponseFormat.<T>builder()
                .data(data)
                .statusCode(HttpStatus.OK.value())
                .message(message)
                .endpoint(endpoint)
                .success(true)
                .errors(null)
                .build();
    }
}
