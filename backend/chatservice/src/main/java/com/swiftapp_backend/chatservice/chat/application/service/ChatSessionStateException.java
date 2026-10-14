package com.swiftapp_backend.chatservice.chat.application.service;

public class ChatSessionStateException extends RuntimeException {
    public ChatSessionStateException(String message) {
        super(message);
    }
}
