package com.swiftapp_backend.ratingservice.shared;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponseFormat<T>{
    private T data;
    private int statusCode;
    private String message;
    private String endpoint;
    private boolean success;
    private Object errors;
}
