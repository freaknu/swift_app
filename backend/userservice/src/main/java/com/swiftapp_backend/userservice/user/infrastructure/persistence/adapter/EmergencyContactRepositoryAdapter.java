package com.swiftapp_backend.userservice.user.infrastructure.persistence.adapter;

import com.swiftapp_backend.userservice.user.domain.model.EmergencyContactModel;
import com.swiftapp_backend.userservice.user.domain.repository.EmergencyContactRepository;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.EmergencyContactEntity;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.UserEntity;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper.EmergencyContactPersistenceMapper;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.repository.SpringDataEmergencyContactRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmergencyContactRepositoryAdapter implements EmergencyContactRepository {
    private final SpringDataEmergencyContactRepository repository;
    private final EmergencyContactPersistenceMapper mapper;
    private final UserPersistenceMapper userMapper;
    private final EntityManager entityManager;

    @Override
    public Optional<EmergencyContactModel> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional
    public List<EmergencyContactModel> saveAll(List<EmergencyContactModel> contacts) {
        List<EmergencyContactEntity> entities = contacts.stream()
                .map(this::toEntity)
                .toList();
        List<EmergencyContactEntity> savedEntities = repository.saveAll(entities);
        List<EmergencyContactModel> savedContacts = new ArrayList<>();
        for (int index = 0; index < savedEntities.size(); index++) {
            EmergencyContactModel saved = mapper.toDomain(savedEntities.get(index));
            saved.setUser(contacts.get(index).getUser());
            savedContacts.add(saved);
        }
        return savedContacts;
    }

    @Override
    public List<EmergencyContactModel> findAllByUser_Id(Long userId) {
        return repository.findAllByUser_Id(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    private EmergencyContactEntity toEntity(EmergencyContactModel model) {
        EmergencyContactEntity entity = mapper.toEntity(model);
        if (model.getUser() != null) {
            if (model.getUser().getId() != null) {
                entity.setUser(entityManager.getReference(UserEntity.class, model.getUser().getId()));
            } else {
                entity.setUser(userMapper.toEntityReference(model.getUser()));
            }
        }
        return entity;
    }

    private EmergencyContactModel toDomain(EmergencyContactEntity entity) {
        EmergencyContactModel model = mapper.toDomain(entity);
        if (entity.getUser() != null) {
            model.setUser(userMapper.toDomainReference(entity.getUser()));
        }
        return model;
    }
}
