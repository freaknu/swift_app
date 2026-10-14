package com.swiftapp_backend.chatservice.chat.application.service;

import com.swiftapp_backend.chatservice.chat.application.port.ChatPort;
import com.swiftapp_backend.chatservice.chat.application.port.ChatIdentityPort;
import com.swiftapp_backend.chatservice.chat.application.port.ChatMessagePublisher;
import com.swiftapp_backend.chatservice.chat.application.port.ChatIdentityPort.Identity;
import com.swiftapp_backend.chatservice.chat.api.dto.SendMessageRequest;
import com.swiftapp_backend.chatservice.chat.domain.enums.ContentType;
import com.swiftapp_backend.chatservice.chat.domain.enums.MessageType;
import com.swiftapp_backend.chatservice.chat.domain.model.ChatSession;
import com.swiftapp_backend.chatservice.chat.domain.model.Message;
import com.swiftapp_backend.chatservice.chat.domain.repository.ChatSessionRepository;
import com.swiftapp_backend.chatservice.chat.domain.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatService implements ChatPort {
    private final ChatSessionRepository chatSessionRepository;
    private final MessageRepository messageRepository;
    private final ChatIdentityPort identityPort;
    private final ChatMessagePublisher messagePublisher;

    @Override
    @Transactional
    public void saveSession(ChatSessionRecord data) {
        openOrUpdateSession(data.rideId(), data.driverId(), data.riderId(), data.rideStatus(),
                data.sessionNameForDriver(), data.sessionNameForRider());
    }

    @Override
    @Transactional
    public void saveMessage(MessageRecord data) {
        Message message = toMessage(data);
        if (message.getMessageId() == null || messageRepository.findById(message.getMessageId()).isPresent()) {
            return;
        }
        messageRepository.saveMessage(message);
    }

    @Override
    public void deleteMessage(Long userId, String messageId) {
        messageRepository.deleteMessage(userId, messageId);
    }

    @Override
    public void updateMessage(MessageRecord data) {
        messageRepository.updateMessage(toMessage(data));
    }

    @Override
    public void markChatSessionInactive(Long sessionId,boolean status) {
        chatSessionRepository.markInactive(sessionId,status);
    }

    @Transactional
    public ChatSession openOrUpdateSession(Long rideId, Long driverId, Long riderId,
                                           String rideStatus, String driverName, String riderName) {
        if (rideId == null || driverId == null || riderId == null) {
            throw new IllegalArgumentException("rideId, driverId and riderId are required to open chat");
        }
        Optional<ChatSession> existing = chatSessionRepository.lockByRideId(rideId);
        if (existing.isPresent()) {
            ChatSession session = existing.get();
            // A late duplicate assignment must never reopen a closed session.
            // Lifecycle closure is the only supported transition to inactive.
            if (!session.isActive() || isTerminal(session.getRideStatus())) {
                return session;
            }
            session.setDriverId(driverId);
            session.setRiderId(riderId);
            session.setRideStatus(rideStatus);
            session.setActive(true);
            if (session.getSessionNameForDriver() == null) {
                session.setSessionNameForDriver(driverName);
            }
            if (session.getSessionNameForRider() == null) {
                session.setSessionNameForRider(riderName);
            }
            chatSessionRepository.saveChatSession(session);
            return session;
        }

        ChatSession session = ChatSession.builder()
                .sessionNameForDriver(driverName != null ? driverName : "ride-" + rideId + "-driver")
                .sessionNameForRider(riderName != null ? riderName : "ride-" + rideId + "-rider")
                .driverId(driverId)
                .riderId(riderId)
                .rideId(rideId)
                .rideStatus(rideStatus)
                .active(true)
                .build();
        chatSessionRepository.saveChatSession(session);
        return session;
    }

    @Transactional
    public void updateRideStatus(Long rideId, String rideStatus) {
        if (rideId == null || rideStatus == null) return;
        chatSessionRepository.lockByRideId(rideId).ifPresent(session -> {
            if (session.isActive() && !isTerminal(session.getRideStatus())) {
                session.setRideStatus(rideStatus);
                chatSessionRepository.saveChatSession(session);
            }
        });
    }

    @Transactional
    public void closeSessionForRide(Long rideId, String rideStatus) {
        closeSessionForRide(rideId, rideStatus, null, null);
    }

    @Transactional
    public void closeSessionForRide(Long rideId, String rideStatus, Long driverId, Long riderId) {
        if (rideId == null) return;
        Optional<ChatSession> existing = chatSessionRepository.lockByRideId(rideId);
        if (existing.isEmpty()) {
            if (driverId == null || riderId == null) return;
            ChatSession closed = ChatSession.builder()
                    .sessionNameForDriver("ride-" + rideId + "-driver")
                    .sessionNameForRider("ride-" + rideId + "-rider")
                    .driverId(driverId)
                    .riderId(riderId)
                    .rideId(rideId)
                    .rideStatus(rideStatus)
                    .active(false)
                    .build();
            chatSessionRepository.saveChatSession(closed);
            return;
        }
        ChatSession session = existing.get();
        if (!session.isActive() && isTerminal(session.getRideStatus())) return;
        session.setRideStatus(rideStatus);
        session.setActive(false);
        chatSessionRepository.saveChatSession(session);
    }

    @Transactional
    public Message sendMessage(Long authUserId, Long sessionId, SendMessageRequest request,
                               MessageType messageType, String replyOfMessageId) {
        ChatSession session = chatSessionRepository.lockById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));
        requireParticipant(authUserId, session);
        if (!session.isActive()) {
            throw new ChatSessionStateException("Chat session is no longer active");
        }
        String messageId = request.getMessageId().trim();
        Optional<Message> existing = messageRepository.findById(messageId);
        if (existing.isPresent()) {
            Message previous = existing.get();
            if (!sessionId.equals(previous.getSessionId())) {
                throw new ChatAuthorizationException("Message id belongs to another chat session");
            }
            return previous;
        }

        String parentId = replyOfMessageId == null || replyOfMessageId.isBlank()
                ? request.getReplyOfMessageId() : replyOfMessageId;
        if (messageType == MessageType.REPLY) {
            if (parentId == null || parentId.isBlank()) {
                throw new IllegalArgumentException("replyOfMessageId is required for a reply");
            }
            Message parent = messageRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Reply target message was not found"));
            if (!sessionId.equals(parent.getSessionId())) {
                throw new ChatAuthorizationException("Reply target belongs to another chat session");
            }
        }
        ContentType contentType = request.getContentType() == null
                ? ContentType.TEXT : request.getContentType();
        String content = request.getMessageContent() == null ? null : request.getMessageContent().trim();
        String url = request.getMessageUrl() == null ? null : request.getMessageUrl().trim();
        if ((content == null || content.isBlank()) && (url == null || url.isBlank())) {
            throw new IllegalArgumentException("messageContent or messageUrl is required");
        }
        LocalDateTime now = LocalDateTime.now();
        Message message = Message.builder()
                .sentById(participantId(authUserId, session))
                .messageId(messageId)
                .messageContent(content)
                .sessionId(sessionId)
                .messageUrl(url)
                .replyOfMessageId(parentId)
                .contentType(contentType)
                .messageType(messageType)
                .createdAt(now)
                .updatedAt(now)
                .build();
        messageRepository.saveMessage(message);
        messagePublisher.publish(message, session);
        return message;
    }

    @Transactional(readOnly = true)
    public void requireParticipant(Long authUserId, Long sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Chat session not found"));
        requireParticipant(authUserId, session);
    }

    @Transactional(readOnly = true)
    public Optional<ChatSession> getSessionByRideId(Long rideId) {
        return chatSessionRepository.findByRideId(rideId);
    }

    @Transactional(readOnly = true)
    public List<Message> getMessages(Long authUserId, Long sessionId, int page, int pageSize) {
        requireParticipant(authUserId, sessionId);
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("page must be >= 0 and size must be between 1 and 100");
        }
        return chatSessionRepository.findAllMessage(sessionId, page, pageSize);
    }

    private void requireParticipant(Long authUserId, ChatSession session) {
        if (authUserId == null || authUserId <= 0) {
            throw new ChatAuthorizationException("Authenticated user is required");
        }
        Identity identity = identityPort.identityForAuthUser(authUserId);
        if (!matchesRider(session, identity) && !matchesDriver(session, identity)) {
            throw new ChatAuthorizationException("Authenticated user is not a participant in this chat");
        }
    }

    private Long participantId(Long authUserId, ChatSession session) {
        Identity identity = identityPort.identityForAuthUser(authUserId);
        if (matchesRider(session, identity)) return session.getRiderId();
        if (matchesDriver(session, identity)) return session.getDriverId();
        throw new ChatAuthorizationException("Authenticated user is not a participant in this chat");
    }

    private boolean matchesRider(ChatSession session, Identity identity) {
        return session.getRiderId() != null && identity != null
                && session.getRiderId().equals(identity.riderId());
    }

    private boolean matchesDriver(ChatSession session, Identity identity) {
        return session.getDriverId() != null && identity != null
                && session.getDriverId().equals(identity.driverId());
    }

    private boolean isTerminal(String status) {
        return "COMPLETED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status);
    }

    private Message toMessage(MessageRecord record) {
        return Message
                .builder()
                .sentById(record.sentById())
                .messageId(record.messageId())
                .messageContent(record.messageContent())
                .sessionId(record.sessionId())
                .messageUrl(record.messageUrl())
                .replyOfMessageId(record.replyOfMessageId())
                .contentType(record.contentType())
                .messageType(record.messageType())
                .readByIds(record.readByIds())
                .isEdited(record.edited())
                .isDeleted(record.deleted())
                .build();
    }
}
