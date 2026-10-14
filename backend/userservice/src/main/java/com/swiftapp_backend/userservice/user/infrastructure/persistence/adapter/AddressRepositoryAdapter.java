package com.swiftapp_backend.userservice.user.infrastructure.persistence.adapter;

import com.swiftapp_backend.userservice.user.domain.model.AddressModel;
import com.swiftapp_backend.userservice.user.domain.repository.AddressRepository;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.AddressEntity;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.UserEntity;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper.AddressPersistenceMapper;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.repository.SpringDataAddressRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressRepositoryAdapter implements AddressRepository {
    private final SpringDataAddressRepository repository;
    private final AddressPersistenceMapper mapper;
    private final UserPersistenceMapper userMapper;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public AddressModel save(AddressModel address) {
        AddressEntity entity = mapper.toEntity(address);
        attachUser(entity, address);
        AddressModel saved = mapper.toDomain(repository.save(entity));
        saved.setUser(address.getUser());
        return saved;
    }

    @Override
    public Optional<AddressModel> findById(Long id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<AddressModel> findAllByUser_Id(Long userId) {
        return repository.findAllByUser_Id(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    private void attachUser(AddressEntity entity, AddressModel model) {
        if (model.getUser() == null) {
            return;
        }

        if (model.getUser().getId() != null) {
            entity.setUser(entityManager.getReference(UserEntity.class, model.getUser().getId()));
        } else {
            entity.setUser(userMapper.toEntityReference(model.getUser()));
        }
    }

    private AddressModel toDomain(AddressEntity entity) {
        AddressModel model = mapper.toDomain(entity);
        if (entity.getUser() != null) {
            model.setUser(userMapper.toDomainReference(entity.getUser()));
        }
        return model;
    }
}
