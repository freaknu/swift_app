package com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper;

import com.swiftapp_backend.userservice.user.domain.model.AddressModel;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.AddressEntity;
import org.springframework.stereotype.Component;

@Component
public class AddressPersistenceMapper {

    public AddressModel toDomain(AddressEntity entity) {
        if (entity == null) {
            return null;
        }

        return AddressModel.builder()
                .id(entity.getId())
                .addressType(entity.getAddressType())
                .label(entity.getLabel())
                .fullAddress(entity.getFullAddress())
                .landmark(entity.getLandmark())
                .city(entity.getCity())
                .state(entity.getState())
                .country(entity.getCountry())
                .pinCode(entity.getPinCode())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .isFavouritePlace(entity.getIsFavouritePlace())
                .defaultAddress(entity.getDefaultAddress())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .active(entity.getActive())
                .build();
    }

    public AddressEntity toEntity(AddressModel model) {
        if (model == null) {
            return null;
        }

        return AddressEntity.builder()
                .id(model.getId())
                .addressType(model.getAddressType())
                .label(model.getLabel())
                .fullAddress(model.getFullAddress())
                .landmark(model.getLandmark())
                .city(model.getCity())
                .state(model.getState())
                .country(model.getCountry())
                .pinCode(model.getPinCode())
                .latitude(model.getLatitude())
                .longitude(model.getLongitude())
                .isFavouritePlace(model.getIsFavouritePlace())
                .defaultAddress(model.getDefaultAddress())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .active(model.getActive())
                .build();
    }
}
