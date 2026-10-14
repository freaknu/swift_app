package com.swiftapp_backend.paymentservice.payment.domain.event;
import java.time.Instant;
public record PaymentStatusEvent(String eventId, Long rideId, Long riderId, Long driverId,
                                 String paymentStatus, String razorpayOrderId, String razorpayPaymentId,
                                 long amount, String currency, Instant eventTime) {}
