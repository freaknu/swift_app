package com.swiftapp_backend.websocketservice.shared;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central error handling for the websocket-service REST controllers
 * (STOMP message handling is out of scope for @RestControllerAdvice).
 * Every failure is logged and returned as an {@link ApiResponseFormat}
 * envelope so UI clients (web/Flutter) always receive a parseable error payload.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponseFormat<Object>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage()));
        log.warn("Constraint violation at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed",
                request.getRequestURI(), errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponseFormat<Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Type mismatch at {}: parameter '{}' got value '{}'",
                request.getRequestURI(), ex.getName(), ex.getValue());
        return buildResponse(HttpStatus.BAD_REQUEST,
                "Invalid value for parameter '" + ex.getName() + "'",
                request.getRequestURI(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponseFormat<Object>> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponseFormat<Object>> handleDataConflict(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data conflict at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(HttpStatus.CONFLICT,
                "The request conflicts with the current state",
                request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseFormat<Object>> handleUnexpectedException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                request.getRequestURI(), null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        log.warn("Request validation failed at {}: {}", uriOf(request), errors);
        return validationResponse("Validation failed", errors, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        log.warn("Unreadable request body at {}: {}", uriOf(request), ex.getMessage());
        return validationResponse("Malformed or invalid JSON request", null, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        log.warn("Missing request parameter at {}: {}", uriOf(request), ex.getParameterName());
        return validationResponse("Missing required parameter '" + ex.getParameterName() + "'",
                null, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        log.warn("Method not supported at {}: {}", uriOf(request), ex.getMethod());
        return errorResponse(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method not supported", null, request);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(
            NoHandlerFoundException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        log.warn("No handler found at {}", uriOf(request));
        return errorResponse(HttpStatus.NOT_FOUND, "Endpoint not found", null, request);
    }

    private ResponseEntity<Object> validationResponse(String message, Object errors, WebRequest request) {
        return errorResponse(HttpStatus.BAD_REQUEST, message, errors, request);
    }

    private ResponseEntity<Object> errorResponse(
            HttpStatus status, String message, Object errors, WebRequest request) {
        return ResponseEntity.status(status).body(response(status, message, uriOf(request), errors));
    }

    private ResponseEntity<ApiResponseFormat<Object>> buildResponse(
            HttpStatus status, String message, String endpoint, Object errors) {
        return ResponseEntity.status(status).body(response(status, message, endpoint, errors));
    }

    private ApiResponseFormat<Object> response(
            HttpStatus status, String message, String endpoint, Object errors) {
        return ApiResponseFormat.builder()
                .success(false).statusCode(status.value()).message(message)
                .endpoint(endpoint).errors(errors).build();
    }

    private String uriOf(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI() : null;
    }
}
