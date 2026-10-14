package com.swiftapp_backend.paymentservice.payment.domain.repository;
import com.swiftapp_backend.paymentservice.payment.domain.model.Payment;
public interface PaymentRepository {
    Payment getByRideId(Long rideId);
    Payment getLockedByRideId(Long rideId);
    Payment insert(Payment payment);
    Payment save(Payment payment);
    void recordOrder(Payment payment);
    Long rideIdForOrder(String orderId);
    boolean orderBelongsToRide(String orderId, Long rideId);
}
