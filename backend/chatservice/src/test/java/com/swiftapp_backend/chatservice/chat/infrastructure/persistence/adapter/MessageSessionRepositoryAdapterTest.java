package com.swiftapp_backend.chatservice.chat.infrastructure.persistence.adapter;

import com.swiftapp_backend.chatservice.chat.domain.enums.ContentType;
import com.swiftapp_backend.chatservice.chat.domain.enums.MessageType;
import com.swiftapp_backend.chatservice.chat.domain.model.Message;
import com.swiftapp_backend.chatservice.chat.infrastructure.persistence.entity.MessageEntity;
import com.swiftapp_backend.chatservice.chat.infrastructure.persistence.repository.MessageJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MessageSessionRepositoryAdapterTest {
    private MessageJpaRepository messageJpaRepository;
    private MessageSessionRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        messageJpaRepository = mock(MessageJpaRepository.class);
        adapter = new MessageSessionRepositoryAdapter(messageJpaRepository);
    }

    @Test
    void savesAllMessageFields() {
        Message message = message();

        adapter.saveMessage(message);

        var entityCaptor = org.mockito.ArgumentCaptor.forClass(MessageEntity.class);
        verify(messageJpaRepository).save(entityCaptor.capture());
        MessageEntity saved = entityCaptor.getValue();
        assertEquals("message-1", saved.getMessageId());
        assertEquals(42L, saved.getSentById());
        assertEquals("hello", saved.getMessageContent());
        assertEquals(101L, saved.getSessionId());
        assertEquals(ContentType.TEXT, saved.getContentType());
        assertEquals(MessageType.MESSAGE, saved.getMessageType());
        assertEquals("42", saved.getReadByIds());
        assertTrue(saved.isEdited());
    }

    @Test
    void updatesAnExistingMessageInsteadOfCreatingASecondRecord() {
        MessageEntity existing = new MessageEntity();
        existing.setMessageId("message-1");
        when(messageJpaRepository.findById("message-1")).thenReturn(Optional.of(existing));

        adapter.updateMessage(message());

        assertEquals("hello", existing.getMessageContent());
        assertEquals(101L, existing.getSessionId());
        verify(messageJpaRepository).save(existing);
    }

    @Test
    void marksDeletedMessagesAsDeleted() {
        MessageEntity existing = new MessageEntity();
        existing.setMessageId("message-1");
        when(messageJpaRepository.findById("message-1")).thenReturn(Optional.of(existing));

        adapter.deleteMessage(42L, "message-1");

        assertTrue(existing.isDeleted());
        verify(messageJpaRepository).save(existing);
    }

    private Message message() {
        return Message.builder()
                .sentById(42L)
                .messageId("message-1")
                .messageContent("hello")
                .sessionId(101L)
                .contentType(ContentType.TEXT)
                .messageType(MessageType.MESSAGE)
                .readByIds("42")
                .isEdited(true)
                .build();
    }
}
