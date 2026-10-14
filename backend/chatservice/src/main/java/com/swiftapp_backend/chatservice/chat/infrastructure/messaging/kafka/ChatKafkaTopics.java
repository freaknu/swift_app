package com.swiftapp_backend.chatservice.chat.infrastructure.messaging.kafka;

public final class ChatKafkaTopics {
    public static final String MESSAGE_CREATED = "message.created";
    public static final String MESSAGE_UPDATED = "message.updated";
    public static final String MESSAGE_DELETED = "message.deleted";
    public static final String MESSAGE_REPLY = "message.reply";
    public static final String CHAT_SESSION_CREATED = "chatsession.created";
    /** Native REST messages are persisted before this event is emitted. */
    public static final String NATIVE_MESSAGE_CREATED = "chat.message.created";

    private ChatKafkaTopics() {
    }
}
