package com.swiftapp_backend.chatservice.chat.infrastructure.persistence.adapter;

import com.swiftapp_backend.chatservice.chat.domain.model.Message;
import com.swiftapp_backend.chatservice.chat.domain.repository.MessageRepository;
import com.swiftapp_backend.chatservice.chat.infrastructure.persistence.entity.MessageEntity;
import com.swiftapp_backend.chatservice.chat.infrastructure.persistence.mapper.MessagePersistenceMapper;
import com.swiftapp_backend.chatservice.chat.infrastructure.persistence.repository.MessageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MessageSessionRepositoryAdapter implements MessageRepository {
    private final MessageJpaRepository messageJpaRepository;

    @Override
    public void saveMessage(Message message) {
        messageJpaRepository.save(MessagePersistenceMapper.toEntity(message));
    }

    @Override
    public Optional<Message> findById(String messageId) {
        return messageJpaRepository.findById(messageId).map(MessagePersistenceMapper::toDomain);
    }

    @Override
    public void deleteMessage(Long deletedBy, String messageId) {
        if (messageId == null) {
            return;
        }

        messageJpaRepository.findById(messageId).ifPresent(entity -> {
            entity.setDeleted(true);
            entity.setDeletedAt(LocalDateTime.now());
            messageJpaRepository.save(entity);
        });
    }

    @Override
    public void updateMessage(Message message) {
        if (message == null || message.getMessageId() == null) {
            return;
        }

        MessageEntity entity = messageJpaRepository.findById(message.getMessageId())
                .orElseGet(() -> MessagePersistenceMapper.toEntity(message));
        MessagePersistenceMapper.copy(message, entity);
        messageJpaRepository.save(entity);
    }
}
