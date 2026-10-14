package com.swiftapp_backend.apigateway.gateway.infrastructure.exception;

import com.swiftapp_backend.apigateway.shared.ApiResponseFormat;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.DispatchExceptionHandler;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

/**
 * Gateway-level error handling (Spring Boot 4 / WebFlux).
 *
 * <p>Catches exceptions raised inside the gateway itself - downstream
 * connection failures, timeouts, filter errors - which never reach a
 * controller. Every error is logged with the request path and returned
 * to clients in the standard {@link ApiResponseFormat} envelope so the
 * React and Flutter UIs can show the message.
 *
 * <p>Downstream services that return their own error envelopes pass
 * through untouched.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayExceptionHandler implements DispatchExceptionHandler {

    private static final MethodParameter RESPONSE_RETURN_TYPE = responseReturnType();

    @Override
    public Mono<HandlerResult> handleError(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.empty();
        }

        String path = exchange.getRequest().getPath().value();
        HttpStatus status = resolveStatus(ex);

        if (status.is5xxServerError()) {
            log.error("Gateway error for {}: {}", path, ex.getMessage(), ex);
        } else {
            log.warn("Gateway client error for {}: {}", path, ex.getMessage());
        }

        ApiResponseFormat<Object> body = ApiResponseFormat.<Object>builder()
                .success(false)
                .statusCode(status.value())
                .message(messageFor(ex, status))
                .endpoint(path)
                .data(null)
                .errors(null)
                .build();

        Mono<ResponseEntity<ApiResponseFormat<Object>>> returnValue =
                Mono.just(ResponseEntity.status(status).body(body));
        return Mono.just(new HandlerResult(this, returnValue, RESPONSE_RETURN_TYPE));
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            HttpStatusCode code = rse.getStatusCode();
            return code instanceof HttpStatus httpStatus ? httpStatus : HttpStatus.valueOf(code.value());
        }
        Throwable cause = ex.getCause();
        if (ex instanceof ConnectException
                || cause instanceof ConnectException
                || ex instanceof TimeoutException
                || cause instanceof TimeoutException) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String messageFor(Throwable ex, HttpStatus status) {
        if (ex instanceof ResponseStatusException rse && rse.getReason() != null) {
            return rse.getReason();
        }
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            return "Service currently unavailable, please try again";
        }
        if (status.is5xxServerError()) {
            return "Internal Server Error";
        }
        return ex.getMessage() != null ? ex.getMessage() : status.getReasonPhrase();
    }

    private static MethodParameter responseReturnType() {
        try {
            Method method = GatewayExceptionHandler.class.getDeclaredMethod("responseEntityMethod");
            return new MethodParameter(method, -1);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Never invoked. Exists only so {@link #responseReturnType()} can
     * capture the generic return type {@code Mono<ResponseEntity<ApiResponseFormat<Object>>>}
     * for the {@link HandlerResult}, letting the normal result handlers
     * render the envelope.
     */
    @SuppressWarnings("unused")
    private Mono<ResponseEntity<ApiResponseFormat<Object>>> responseEntityMethod() {
        throw new UnsupportedOperationException();
    }
}
