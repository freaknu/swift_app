package com.swiftapp_backend.chatservice.chat.api.dto;

import com.swiftapp_backend.chatservice.chat.domain.enums.ContentType;
import com.swiftapp_backend.chatservice.chat.domain.enums.MessageType;
import com.swiftapp_backend.chatservice.chat.domain.model.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private String messageId;
    private Long sentById;
    private String messageContent;
    private Long sessionId;
    private String messageUrl;
    private String replyOfMessageId;
    private ContentType contentType;
    private MessageType messageType;
    private String readByIds;
    private boolean edited;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MessageDto fromDomain(Message message) {
        return MessageDto.builder()
                .messageId(message.getMessageId())
                .sentById(message.getSentById())
                .messageContent(message.getMessageContent())
                .sessionId(message.getSessionId())
                .messageUrl(message.getMessageUrl())
                .replyOfMessageId(message.getReplyOfMessageId())
                .contentType(message.getContentType())
                .messageType(message.getMessageType())
                .readByIds(message.getReadByIds())
                .edited(message.isEdited())
                .deleted(message.isDeleted())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }
}
