package com.swiftapp_backend.paymentservice.shared;
import com.swiftapp_backend.paymentservice.payment.domain.exception.PaymentException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(PaymentException.class)
    public ResponseEntity<ApiResponseFormat<Void>> paymentError(PaymentException ex, HttpServletRequest request) {
        int status = switch (ex.getKind()) {
            case INVALID_REQUEST -> 400;
            case FORBIDDEN -> 403;
            case NOT_FOUND -> 404;
            case CONFLICT -> 409;
            case UNAVAILABLE -> 503;
            case PROVIDER_FAILURE -> 502;
        };
        return ResponseEntity.status(status).body(ApiResponseFormat.failure(status, ex.getMessage(), request.getRequestURI(), null));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, MissingRequestHeaderException.class,
                       HttpMessageNotReadableException.class, ConstraintViolationException.class,
                       org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponseFormat<Void>> invalidRequest(Exception ex, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiResponseFormat.failure(400, "Invalid payment request", request.getRequestURI(), null));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseFormat<Void>> unexpectedError(Exception ex, HttpServletRequest request) {
        log.error("Unexpected payment failure method={} endpoint={} failure={}",
            request.getMethod(), request.getRequestURI(), SafeFailureDetails.describe(ex));
        return ResponseEntity.internalServerError().body(ApiResponseFormat.failure(500, "Payment processing failed; please retry", request.getRequestURI(), null));
    }
}
