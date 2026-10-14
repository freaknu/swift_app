package com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper;

import com.swiftapp_backend.userservice.user.domain.model.AddressModel;
import com.swiftapp_backend.userservice.user.domain.model.EmergencyContactModel;
import com.swiftapp_backend.userservice.user.domain.model.UserModel;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.AddressEntity;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.EmergencyContactEntity;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserPersistenceMapper {
    private final AddressPersistenceMapper addressMapper;
    private final EmergencyContactPersistenceMapper emergencyContactMapper;

    public UserModel toDomain(UserEntity entity) {
        UserModel model = toDomainReference(entity);
        if (model == null) {
            return null;
        }

        if (entity.getAddresses() != null) {
            List<AddressModel> addresses = new ArrayList<>();
            for (AddressEntity addressEntity : entity.getAddresses()) {
                AddressModel address = addressMapper.toDomain(addressEntity);
                address.setUser(model);
                addresses.add(address);
            }
            model.setAddresses(addresses);
        }

        if (entity.getEmergencyContacts() != null) {
            List<EmergencyContactModel> contacts = new ArrayList<>();
            for (EmergencyContactEntity contactEntity : entity.getEmergencyContacts()) {
                EmergencyContactModel contact = emergencyContactMapper.toDomain(contactEntity);
                contact.setUser(model);
                contacts.add(contact);
            }
            model.setEmergencyContacts(contacts);
        }

        return model;
    }

    public UserModel toDomainReference(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserModel.builder()
                .id(entity.getId())
                .authUserId(entity.getAuthUserId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phoneNumber(entity.getPhoneNumber())
                .profileImageUrl(entity.getProfileImageUrl())
                .active(entity.getActive())
                .deleted(entity.getDeleted())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .walletId(entity.getWalletId())
                .build();
    }

    public UserEntity toEntity(UserModel model) {
        UserEntity entity = toEntityReference(model);
        if (entity == null) {
            return null;
        }

        if (model.getAddresses() != null) {
            List<AddressEntity> addresses = new ArrayList<>();
            for (AddressModel addressModel : model.getAddresses()) {
                AddressEntity address = addressMapper.toEntity(addressModel);
                address.setUser(entity);
                addresses.add(address);
            }
            entity.setAddresses(addresses);
        }

        if (model.getEmergencyContacts() != null) {
            List<EmergencyContactEntity> contacts = new ArrayList<>();
            for (EmergencyContactModel contactModel : model.getEmergencyContacts()) {
                EmergencyContactEntity contact = emergencyContactMapper.toEntity(contactModel);
                contact.setUser(entity);
                contacts.add(contact);
            }
            entity.setEmergencyContacts(contacts);
        }

        return entity;
    }

    public UserEntity toEntityReference(UserModel model) {
        if (model == null) {
            return null;
        }

        return UserEntity.builder()
                .id(model.getId())
                .authUserId(model.getAuthUserId())
                .fullName(model.getFullName())
                .email(model.getEmail())
                .phoneNumber(model.getPhoneNumber())
                .profileImageUrl(model.getProfileImageUrl())
                .active(model.getActive())
                .deleted(model.getDeleted())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .walletId(model.getWalletId())
                .build();
    }
}
