package com.swiftapp_backend.chatservice.chat.domain.model;

import com.swiftapp_backend.chatservice.chat.domain.enums.ContentType;
import com.swiftapp_backend.chatservice.chat.domain.enums.MessageType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class Message {
    private Long sentById;
    private String messageId;
    private String messageContent;
    private Long sessionId;
    // if message type is media
    private String messageUrl;
    // if it is reply of message id
    private String replyOfMessageId;
    // content type of the message
    private ContentType contentType;
    // is message or reply
    private MessageType messageType;
    private String readByIds;
    private boolean isEdited;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
