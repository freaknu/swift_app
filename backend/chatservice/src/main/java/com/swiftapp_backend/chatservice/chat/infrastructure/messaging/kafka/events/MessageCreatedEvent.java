package com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka.events;

import com.swiftapp_backend.chatservice.chat.domain.enums.ContentType;
import com.swiftapp_backend.chatservice.chat.domain.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class MessageCreatedEvent {
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
    /** Ride/session context used by notification-service for the other party. */
    private Long rideId;
    private Long riderId;
    private Long driverId;
    private Long recipientId;
    private String recipientRole;
}
