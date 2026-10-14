package com.swiftapp_backend.websocketservice.realtime.api.controller;

import com.swiftapp_backend.websocketservice.realtime.api.dto.ChatMessageDto;
import com.swiftapp_backend.websocketservice.realtime.infrastructure.config.ChatMessageWriteClient;
import com.swiftapp_backend.websocketservice.realtime.infrastructure.config.DriverSubscriptionInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {
    private final ChatMessageWriteClient chatMessageWriteClient;

    @MessageMapping("/chat.send")
    public void send(ChatMessageDto dto, SimpMessageHeaderAccessor headers) {
        log.debug("chat.send messageId={} sessionId={}", dto.getMessageId(), dto.getSessionId());
        chatMessageWriteClient.send(authUserId(headers), dto, false);
    }

    @MessageMapping("/chat.reply")
    public void reply(ChatMessageDto dto, SimpMessageHeaderAccessor headers) {
        log.debug("chat.reply messageId={} sessionId={}", dto.getMessageId(), dto.getSessionId());
        chatMessageWriteClient.send(authUserId(headers), dto, true);
    }

    @MessageMapping("/chat.update")
    public void update(ChatMessageDto dto) {
        throw new MessagingException("Legacy chat edits are not supported");
    }

    @MessageMapping("/chat.delete")
    public void delete(ChatMessageDto dto) {
        throw new MessagingException("Legacy chat deletion is not supported");
    }

    @MessageExceptionHandler(MessagingException.class)
    @SendToUser(value = "/queue/chat-errors", broadcast = false)
    public Map<String, String> rejectedWrite(MessagingException exception) {
        // Never return upstream response bodies, messages, or identity headers.
        return Map.of("error", "CHAT_WRITE_REJECTED");
    }

    private Long authUserId(SimpMessageHeaderAccessor headers) {
        Object identity = headers.getSessionAttributes() == null ? null
                : headers.getSessionAttributes().get(DriverSubscriptionInterceptor.AUTH_USER_ID_ATTRIBUTE);
        if (!(identity instanceof Long userId) || userId <= 0) {
            throw new MessagingException("Authorization required for chat write");
        }
        return userId;
    }
}
