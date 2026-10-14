package com.swiftapp_backend.driverservice.driver.application.service;

public class MatchingDecisionUnavailableException extends RuntimeException {
    public MatchingDecisionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
