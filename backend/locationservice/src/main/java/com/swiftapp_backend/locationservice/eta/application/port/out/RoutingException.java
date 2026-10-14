package com.swiftapp_backend.locationservice.eta.application.port.out;

/** Provider-neutral failure contract. No HTTP or Google types escape the adapter. */
public class RoutingException extends RuntimeException {
    public enum Reason { NO_ROUTE, UNAVAILABLE, TIMEOUT, INVALID_RESPONSE }

    private final Reason reason;

    public RoutingException(Reason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
