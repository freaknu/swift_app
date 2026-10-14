package com.swiftapp_backend.paymentservice.payment.api.dto;
import com.swiftapp_backend.paymentservice.payment.domain.model.Payment;
import com.swiftapp_backend.paymentservice.payment.domain.enums.PaymentStatus;
public record PaymentResponse(Long rideId, Long riderId, Long driverId, PaymentStatus status,
                              String razorpayKeyId, String razorpayOrderId, long amount,
                              String currency, String razorpayPaymentId) {
    public static PaymentResponse from(Payment payment, String keyId) {
        return new PaymentResponse(payment.getRideId(), payment.getRiderId(), payment.getDriverId(), payment.getStatus(),
            keyId, payment.getRazorpayOrderId(), payment.getAmount(), payment.getCurrency(), payment.getRazorpayPaymentId());
    }
}
