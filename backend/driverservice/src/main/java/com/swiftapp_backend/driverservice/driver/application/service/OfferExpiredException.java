package com.swiftapp_backend.driverservice.driver.application.service;

/** Thrown when the driver answers an offer after its window closed. */
public class OfferExpiredException extends RuntimeException {
    public OfferExpiredException(String message) {
        super(message);
    }
}
