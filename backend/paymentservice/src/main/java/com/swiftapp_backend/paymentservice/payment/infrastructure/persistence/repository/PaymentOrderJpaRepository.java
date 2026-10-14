package com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.repository;
import com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.entity.PaymentOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PaymentOrderJpaRepository extends JpaRepository<PaymentOrderEntity, String> {}
