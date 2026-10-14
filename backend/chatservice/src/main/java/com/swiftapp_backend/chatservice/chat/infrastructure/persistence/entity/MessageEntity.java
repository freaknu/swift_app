package com.swiftapp_backend.chatservice.chat.infrastructure.persistence.entity;

import com.swiftapp_backend.chatservice.chat.domain.enums.ContentType;
import com.swiftapp_backend.chatservice.chat.domain.enums.MessageType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "message",indexes = {
        @Index(name = "message_sent_by_idx",columnList = "sent_by_id"),
        @Index(name = "message_session_idx",columnList = "session_id"),
        @Index(name = "message_reply_idx",columnList = "reply_by_messageid"),
        @Index(name = "message_type_idx",columnList = "message_type")
})
@RequiredArgsConstructor
public class MessageEntity {
    @Id
    private String messageId;
    @Column(name = "sent_by_id")
    private Long sentById;
    private String messageContent;
    @Column(name = "session_id")
    private Long sessionId;
    // if message type is media
    private String messageUrl;
    // if it is reply of message id
    @Column(name = "reply_by_messageid")
    private String replyOfMessageId;
    // content type of the message
    @Enumerated(EnumType.STRING)
    private ContentType contentType;
    // is message or reply
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type")
    private MessageType messageType;

    // comma separated
    private String readByIds;
    private boolean isEdited;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isDeleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
