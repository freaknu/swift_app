package com.swiftapp_backend.chatservice.chat.api.controller;

import com.swiftapp_backend.chatservice.chat.api.dto.ChatSessionDto;
import com.swiftapp_backend.chatservice.chat.api.dto.MessageDto;
import com.swiftapp_backend.chatservice.chat.api.dto.SendMessageRequest;
import com.swiftapp_backend.chatservice.chat.application.service.ChatService;
import com.swiftapp_backend.chatservice.chat.domain.enums.MessageType;
import com.swiftapp_backend.chatservice.chat.domain.model.ChatSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {
    private final ChatService chatService;

    /**
     * Chat history for a session, newest first.
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        List<MessageDto> messages = chatService.getMessages(authUserId, sessionId, page, size)
                .stream()
                .map(MessageDto::fromDomain)
                .toList();
        return ResponseEntity.ok(messages);
    }

    /**
     * Persist and fan out a message. The authenticated gateway identity is
     * used as the sender; sentById is intentionally not accepted from JSON.
     */
    @PostMapping("/sessions/{sessionId}/messages")
    public ResponseEntity<MessageDto> sendMessage(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable Long sessionId,
            @Valid @RequestBody SendMessageRequest request) {
        MessageType type = request.getReplyOfMessageId() == null
                || request.getReplyOfMessageId().isBlank()
                ? MessageType.MESSAGE : MessageType.REPLY;
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageDto.fromDomain(chatService.sendMessage(
                        authUserId, sessionId, request, type, request.getReplyOfMessageId())));
    }

    /** Explicit reply form for clients that prefer a path parent id. */
    @PostMapping("/sessions/{sessionId}/messages/{messageId}/reply")
    public ResponseEntity<MessageDto> reply(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable Long sessionId,
            @PathVariable String messageId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageDto.fromDomain(chatService.sendMessage(
                        authUserId, sessionId, request, MessageType.REPLY, messageId)));
    }

    /**
     * Chat session for a ride, including a closed session for history access.
     */
    @GetMapping("/sessions/by-ride/{rideId}")
    public ResponseEntity<ChatSessionDto> getSessionByRide(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable Long rideId) {
        ChatSession session = chatService.getSessionByRideId(rideId)
                .orElse(null);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        chatService.requireParticipant(authUserId, session.getSessionId());
        return ResponseEntity.ok(ChatSessionDto.fromDomain(session));
    }

    /**
     * Internal authorization probe used by websocket-service before allowing
     * a client to subscribe to a chat topic. It intentionally returns no
     * session data; callers only need a participant/not-a-participant result.
     */
    @GetMapping("/sessions/{sessionId}/access")
    public ResponseEntity<Void> checkSessionAccess(
            @RequestHeader("x-user-id") Long authUserId,
            @PathVariable Long sessionId) {
        chatService.requireParticipant(authUserId, sessionId);
        return ResponseEntity.noContent().build();
    }
}
