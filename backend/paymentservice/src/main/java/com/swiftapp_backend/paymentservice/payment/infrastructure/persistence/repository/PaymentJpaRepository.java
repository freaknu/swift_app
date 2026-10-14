package com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.repository;
import com.swiftapp_backend.paymentservice.payment.infrastructure.persistence.entity.PaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentEntity p where p.rideId = :rideId")
    Optional<PaymentEntity> findLocked(@Param("rideId") Long rideId);
}
