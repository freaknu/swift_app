package com.swiftapp_backend.paymentservice.shared;
import java.time.OffsetDateTime;
public record ApiResponseFormat<T>(T data, int statusCode, String message, String endpoint,
                                   boolean success, Object errors, OffsetDateTime timestamp) {
    public static <T> ApiResponseFormat<T> success(T data, String endpoint) {
        return new ApiResponseFormat<>(data, 200, "Payment details fetched", endpoint, true, null, OffsetDateTime.now());
    }
    public static ApiResponseFormat<Void> failure(int status, String message, String endpoint, Object errors) {
        return new ApiResponseFormat<>(null, status, message, endpoint, false, errors, OffsetDateTime.now());
    }
}
