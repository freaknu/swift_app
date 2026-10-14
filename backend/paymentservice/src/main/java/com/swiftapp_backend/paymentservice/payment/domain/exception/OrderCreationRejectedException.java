package com.swiftapp_backend.paymentservice.payment.domain.exception;

/** A definite rejection before an order was created, so submitting the same receipt again is safe. */
public class OrderCreationRejectedException extends PaymentException {
    public OrderCreationRejectedException(Kind kind, String message) { super(kind, message); }
}
