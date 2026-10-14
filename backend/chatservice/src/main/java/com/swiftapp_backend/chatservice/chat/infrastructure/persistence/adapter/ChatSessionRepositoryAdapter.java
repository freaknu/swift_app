package com.swiftapp_backend.chatservice.chat.infrastructure.persistence.adapter;

import com.swiftapp_backend.chatservice.chat.domain.model.ChatSession;
import com.swiftapp_backend.chatservice.chat.domain.model.Message;
import com.swiftapp_backend.chatservice.chat.domain.repository.ChatSessionRepository;
import com.swiftapp_backend.chatservice.chat.infrastructure.persistence.entity.ChatSessionEntity;
import com.swiftapp_backend.chatservice.chat.infrastructure.persistence.mapper.ChatSessionPersistenceMapper;
import com.swiftapp_backend.chatservice.chat.infrastructure.persistence.mapper.MessagePersistenceMapper;
import com.swiftapp_backend.chatservice.chat.infrastructure.persistence.repository.ChatSessionJpaRepository;
import com.swiftapp_backend.chatservice.chat.infrastructure.persistence.repository.MessageJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
@RequiredArgsConstructor
public class ChatSessionRepositoryAdapter implements ChatSessionRepository {
    private final ChatSessionJpaRepository chatSessionJpaRepository;
    private final MessageJpaRepository messageJpaRepository;

    @Override
    public void saveChatSession(ChatSession session) {
        ChatSessionEntity chat = chatSessionJpaRepository.findByRideId(session.getRideId())
                .orElseGet(() -> ChatSessionPersistenceMapper.toEntity(session));
        chat.setSessionNameForDriver(session.getSessionNameForDriver());
        chat.setSessionNameForRider(session.getSessionNameForRider());
        chat.setDriverId(session.getDriverId());
        chat.setRiderId(session.getRiderId());
        chat.setRideId(session.getRideId());
        chat.setRideStatus(session.getRideStatus());
        chat.setActive(session.isActive());
        ChatSessionEntity saved = chatSessionJpaRepository.save(chat);
        if (saved != null) {
            session.setSessionId(saved.getId());
        }
    }

    @Override
    public void markInactive(Long sessionId,boolean status) {
        chatSessionJpaRepository.findById(sessionId).ifPresent(chatSession -> {
            chatSession.setActive(status);
            chatSessionJpaRepository.save(chatSession);
        });
    }

    @Override
    public Optional<ChatSession> getChatSession(Long rideId) {
        return chatSessionJpaRepository.findByRideIdAndIsActiveTrue(rideId)
                .map(ChatSessionPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ChatSession> findByRideId(Long rideId) {
        return chatSessionJpaRepository.findByRideId(rideId)
                .map(ChatSessionPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ChatSession> findById(Long sessionId) {
        return chatSessionJpaRepository.findById(sessionId)
                .map(ChatSessionPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ChatSession> lockByRideId(Long rideId) {
        return chatSessionJpaRepository.lockByRideId(rideId).map(ChatSessionPersistenceMapper::toDomain);
    }

    @Override
    public Optional<ChatSession> lockById(Long sessionId) {
        return chatSessionJpaRepository.lockById(sessionId).map(ChatSessionPersistenceMapper::toDomain);
    }

    @Override
    public List<Message> findAllMessage(Long chatSessionId,int page,int pageSize) {
        Pageable pageable = PageRequest.of(
                page,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        return messageJpaRepository.findAllBySessionId(chatSessionId,pageable)
                .stream()
                .map(MessagePersistenceMapper::toDomain)
                .toList();
    }
}
