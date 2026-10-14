package com.swiftapp_backend.websocketservice.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseFormat<T> {
    private T data;
    private boolean success;
    private int statusCode;
    private String message;
    private Object errors;
    private String endpoint;
}
