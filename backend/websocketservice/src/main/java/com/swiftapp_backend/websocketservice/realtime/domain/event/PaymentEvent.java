package com.swiftapp_backend.websocketservice.realtime.domain.event;

/** Public payment status only; never includes gateway credentials or signatures. */
public record PaymentEvent(
        String eventId,
        String eventType,
        Long rideId,
        Long riderId,
        Long driverId,
        String paymentStatus,
        String razorpayOrderId,
        String razorpayPaymentId,
        Long amount,
        String currency,
        String eventTime
) implements RealtimeEvent {
}
