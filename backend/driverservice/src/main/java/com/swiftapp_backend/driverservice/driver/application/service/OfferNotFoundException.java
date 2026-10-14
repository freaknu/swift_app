package com.swiftapp_backend.driverservice.driver.application.service;

/** Thrown when the driver has no pending offer for the given ride. */
public class OfferNotFoundException extends RuntimeException {
    public OfferNotFoundException(String message) {
        super(message);
    }
}
