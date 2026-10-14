package com.swiftapp_backend.chatservice.chat.infrastructure.persistence.mapper;

import com.swiftapp_backend.chatservice.chat.domain.model.Message;
import com.swiftapp_backend.chatservice.chat.infrastructure.persistence.entity.MessageEntity;

public final class MessagePersistenceMapper {
    private MessagePersistenceMapper() {
    }

    public static MessageEntity toEntity(Message domain) {
        MessageEntity entity = new MessageEntity();
        copy(domain, entity);
        return entity;
    }

    public static Message toDomain(MessageEntity entity) {
        Message message = Message.builder()
                .sentById(entity.getSentById())
                .messageId(entity.getMessageId())
                .messageContent(entity.getMessageContent())
                .sessionId(entity.getSessionId())
                .messageUrl(entity.getMessageUrl())
                .replyOfMessageId(entity.getReplyOfMessageId())
                .contentType(entity.getContentType())
                .messageType(entity.getMessageType())
                .readByIds(entity.getReadByIds())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
        message.setEdited(entity.isEdited());
        message.setDeleted(entity.isDeleted());
        return message;
    }

    public static void copy(Message domain, MessageEntity entity) {
        entity.setMessageId(domain.getMessageId());
        entity.setSentById(domain.getSentById());
        entity.setMessageContent(domain.getMessageContent());
        entity.setSessionId(domain.getSessionId());
        entity.setMessageUrl(domain.getMessageUrl());
        entity.setReplyOfMessageId(domain.getReplyOfMessageId());
        entity.setContentType(domain.getContentType());
        entity.setMessageType(domain.getMessageType());
        entity.setReadByIds(domain.getReadByIds());
        entity.setEdited(domain.isEdited());
        entity.setDeleted(domain.isDeleted());

        if (domain.getCreatedAt() != null) {
            entity.setCreatedAt(domain.getCreatedAt());
        }
        if (domain.getUpdatedAt() != null) {
            entity.setUpdatedAt(domain.getUpdatedAt());
        }
        if (domain.getDeletedAt() != null) {
            entity.setDeletedAt(domain.getDeletedAt());
        }
    }
}
