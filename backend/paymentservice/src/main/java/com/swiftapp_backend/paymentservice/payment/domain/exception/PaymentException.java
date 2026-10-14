package com.swiftapp_backend.paymentservice.payment.domain.exception;
public class PaymentException extends RuntimeException {
    public enum Kind { INVALID_REQUEST, FORBIDDEN, NOT_FOUND, CONFLICT, UNAVAILABLE, PROVIDER_FAILURE }
    private final Kind kind;
    public PaymentException(Kind kind, String message) { super(message); this.kind = kind; }
    public Kind getKind() { return kind; }
}
