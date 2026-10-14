package com.swiftapp_backend.paymentservice.payment.application.port;
import java.util.function.Supplier;
public interface PaymentUnitOfWork {
    <T> T execute(Supplier<T> work);
    /** Suspend any caller transaction while coordinating remote provider work. */
    <T> T outside(Supplier<T> work);
}
