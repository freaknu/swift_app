package com.swiftapp_backend.matchingservice.shared;




import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponseFormat<Object>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Constraint violation at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }


    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponseFormat<Object>> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request
    ) {
        log.warn("Runtime exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseFormat<Object>> handleException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unhandled exception at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error.",
                request.getRequestURI(),
                null
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("Validation failed at {}: {}", endpoint(request), ex.getMessage(), ex);

        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        ApiResponseFormat<Object> response = ApiResponseFormat.builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message("Validation Failed")
                .errors(errors)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        log.warn("Malformed JSON request at {}: {}", endpoint(request), ex.getMessage(), ex);

        ApiResponseFormat<Object> response = ApiResponseFormat.builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message("Malformed JSON Request")
                .errors(null)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    private String endpoint(WebRequest request) {
        if (request instanceof ServletWebRequest servletRequest) {
            return servletRequest.getRequest().getRequestURI();
        }
        return null;
    }

    private ResponseEntity<ApiResponseFormat<Object>> buildResponse(
            HttpStatus status,
            String message,
            String endpoint,
            Object errors
    ) {

        ApiResponseFormat<Object> response = ApiResponseFormat.builder()
                .success(false)
                .statusCode(status.value())
                .message(message)
                .endpoint(endpoint)
                .errors(errors)
                .build();

        return ResponseEntity.status(status).body(response);
    }
}
