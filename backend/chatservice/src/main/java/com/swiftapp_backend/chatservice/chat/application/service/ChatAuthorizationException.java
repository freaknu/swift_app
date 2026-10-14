package com.swiftapp_backend.chatservice.chat.application.service;

public class ChatAuthorizationException extends RuntimeException {
    public ChatAuthorizationException(String message) {
        super(message);
    }
}
