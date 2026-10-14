package com.swiftapp_backend.userservice.user.infrastructure.persistence.repository;

import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.RideHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataRideHistoryRepository extends JpaRepository<RideHistoryEntity, Long> {
    List<RideHistoryEntity> findByAuthUserIdOrderByCreatedAtDesc(Long authUserId);

    List<RideHistoryEntity> findByAuthUserIdInOrderByCreatedAtDesc(List<Long> authUserIds);
}
