package com.swiftapp_backend.analyticalservice.shared;

public class AnalyticsAccessDeniedException extends RuntimeException {
    public AnalyticsAccessDeniedException(String message) {
        super(message);
    }
}
