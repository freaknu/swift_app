package com.swiftapp_backend.apigateway.gateway.api.controller;

import com.swiftapp_backend.apigateway.gateway.application.service.FallbackService;
import com.swiftapp_backend.apigateway.shared.ApiResponseFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class FallbackController {
    private final FallbackService fallbackService;

    @RequestMapping("/fallback/auth")
    public Mono<ResponseEntity<ApiResponseFormat<Object>>> authFallback() {

        ApiResponseFormat<Object> response = ApiResponseFormat.<Object>builder()
                .success(false)
                .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message(fallbackService.unavailableMessage("Auth service"))
                .data(null)
                .build();

        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(response)
        );
    }


    @RequestMapping("/fallback/rider")
    public Mono<ResponseEntity<ApiResponseFormat<Object>>> riderFallback() {
        ApiResponseFormat<Object>response = ApiResponseFormat.builder()
                .success(false)
                .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                .message(fallbackService.unavailableMessage("Ride Service"))
                .data(null)
                .build();
        return Mono
                .just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(response));
    }

        @RequestMapping({"/fallback/user", "/fallback/location", "/fallback/driver", "/fallback/rating", "/fallback/analytics","/fallback/matching"})
        public Mono<ResponseEntity<ApiResponseFormat<Object>>> serviceFallback(
                        org.springframework.web.server.ServerWebExchange exchange) {
                String path = exchange.getRequest().getPath().value();
                String service = path.substring(path.lastIndexOf('/') + 1);
                ApiResponseFormat<Object> response = ApiResponseFormat.builder()
                                .success(false)
                                .statusCode(HttpStatus.SERVICE_UNAVAILABLE.value())
                                .message(fallbackService.unavailableMessage(service + " service"))
                                .data(null)
                                .build();
                return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response));
        }
}
