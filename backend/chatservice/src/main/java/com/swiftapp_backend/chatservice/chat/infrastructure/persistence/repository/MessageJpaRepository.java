package com.swiftapp_backend.chatservice.chat.infrastructure.persistence.repository;

import com.swiftapp_backend.chatservice.chat.infrastructure.persistence.entity.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageJpaRepository extends JpaRepository<MessageEntity, String> {
    Page<MessageEntity> findAllBySessionId(Long sessionId, Pageable pageable);
}
