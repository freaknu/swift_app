package com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.adapter;
import com.swiftapp_backend.paymentservice.payment.domain.model.Payment;
import com.swiftapp_backend.paymentservice.payment.domain.repository.PaymentRepository;
import com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.entity.*;
import com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
@Repository @RequiredArgsConstructor
public class JpaPaymentRepository implements PaymentRepository {
    private final jakarta.persistence.EntityManager entityManager;
    private final PaymentJpaRepository payments;
    private final PaymentOrderJpaRepository orders;
    @Override public Payment getByRideId(Long id) { return payments.findById(id).map(this::model).orElse(null); }
    @Override public Payment getLockedByRideId(Long id) { return payments.findLocked(id).map(this::model).orElse(null); }
    @Override public Payment insert(Payment payment) {
        PaymentEntity entity = entity(payment, new PaymentEntity());
        entityManager.persist(entity);
        entityManager.flush();
        return model(entity);
    }
    @Override public Payment save(Payment payment) {
        PaymentEntity entity = payments.findById(payment.getRideId()).orElseThrow();
        return model(payments.saveAndFlush(entity(payment, entity)));
    }
    private PaymentEntity entity(Payment payment, PaymentEntity entity) {
        entity.setRideId(payment.getRideId()); entity.setRiderId(payment.getRiderId()); entity.setDriverId(payment.getDriverId());
        entity.setAmount(payment.getAmount()); entity.setCurrency(payment.getCurrency()); entity.setReceipt(payment.getReceipt());
        entity.setRazorpayOrderId(payment.getRazorpayOrderId()); entity.setRazorpayPaymentId(payment.getRazorpayPaymentId());
        entity.setStatus(payment.getStatus()); entity.setCreatedAt(payment.getCreatedAt()); entity.setUpdatedAt(payment.getUpdatedAt());
        entity.setOrderCreationAttemptedAt(payment.getOrderCreationAttemptedAt());
        entity.setProviderOperationToken(payment.getProviderOperationToken());
        entity.setProviderOperationExpiresAt(payment.getProviderOperationExpiresAt());
        entity.setProviderWorkflowVersion(payment.getProviderWorkflowVersion());
        return entity;
    }
    @Override public void recordOrder(Payment payment) {
        orders.save(new PaymentOrderEntity(payment.getRazorpayOrderId(), payment.getRideId(), payment.getReceipt()));
    }
    @Override public Long rideIdForOrder(String orderId) { return orders.findById(orderId).map(PaymentOrderEntity::getRideId).orElse(null); }
    @Override public boolean orderBelongsToRide(String orderId, Long rideId) { return rideId.equals(rideIdForOrder(orderId)); }
    private Payment model(PaymentEntity e) {
        return Payment.builder().rideId(e.getRideId()).riderId(e.getRiderId()).driverId(e.getDriverId()).amount(e.getAmount())
            .currency(e.getCurrency()).receipt(e.getReceipt()).razorpayOrderId(e.getRazorpayOrderId())
            .razorpayPaymentId(e.getRazorpayPaymentId()).status(e.getStatus()).createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
            .orderCreationAttemptedAt(e.getOrderCreationAttemptedAt()).providerOperationToken(e.getProviderOperationToken())
            .providerOperationExpiresAt(e.getProviderOperationExpiresAt()).providerWorkflowVersion(e.getProviderWorkflowVersion()).build();
    }
}
