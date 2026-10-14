package com.swiftapp_backend.chatservice.chat.infrastructure.persistence.repository;

import com.swiftapp_backend.chatservice.chat.infrastructure.persistence.entity.ChatSessionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatSessionJpaRepository extends JpaRepository<ChatSessionEntity,Long> {
    Optional<ChatSessionEntity> findByRideIdAndIsActiveTrue(Long rideId);
    Optional<ChatSessionEntity> findByRideId(Long rideId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ChatSessionEntity s where s.rideId = :rideId")
    Optional<ChatSessionEntity> lockByRideId(@Param("rideId") Long rideId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ChatSessionEntity s where s.id = :id")
    Optional<ChatSessionEntity> lockById(@Param("id") Long id);
}
