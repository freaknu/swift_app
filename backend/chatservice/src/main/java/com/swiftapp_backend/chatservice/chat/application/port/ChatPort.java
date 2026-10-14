package com.swiftapp_backend.chatservice.chat.application.port;

import com.swiftapp_backend.chatservice.chat.domain.enums.ContentType;
import com.swiftapp_backend.chatservice.chat.domain.enums.MessageType;

public interface ChatPort {
    void saveSession(ChatSessionRecord data);
    void saveMessage(MessageRecord data);
    void deleteMessage(Long userId, String messageId);
    void updateMessage(MessageRecord data);
    void markChatSessionInactive(Long sessionId,boolean status);
    public record ChatSessionRecord(
            String sessionNameForDriver,
            String sessionNameForRider,
            Long driverId,
            Long riderId,
            Long rideId,
            String rideStatus
    ){ }

    public record MessageRecord(Long sentById,
            String messageId,
            String messageContent,
            Long sessionId,
            // if message type is media
            String messageUrl,
            // if it is reply of message id
            String replyOfMessageId,
            // content type of the message
            ContentType contentType,
            // is message or reply
            MessageType messageType,
            // comma-separated user ids that have read the message
            String readByIds,
            boolean edited,
            boolean deleted) {
        public MessageRecord(Long sentById,
                String messageId,
                String messageContent,
                Long sessionId,
                String messageUrl,
                String replyOfMessageId,
                ContentType contentType,
                MessageType messageType) {
            this(sentById, messageId, messageContent, sessionId, messageUrl,
                    replyOfMessageId, contentType, messageType, null, false, false);
        }
    }
}
