package com.swiftapp_backend.websocketservice.realtime.api.dto;

import com.swiftapp_backend.websocketservice.realtime.domain.enums.ContentType;
import com.swiftapp_backend.websocketservice.realtime.domain.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class ChatMessageDto {
    /**
     * Generated on the Flutter side. The backend never creates it —
     * it is the idempotency key for the whole pipeline.
     */
    private String messageId;
    private Long sentById;
    private String messageContent;
    private Long sessionId;
    // if message type is media
    private String messageUrl;
    // if it is a reply
    private String replyOfMessageId;
    private ContentType contentType;
    private MessageType messageType;
    private String readByIds;
    private boolean edited;
    private boolean deleted;
}
