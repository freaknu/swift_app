package com.swiftapp_backend.userservice.user.application.mapper;

import com.swiftapp_backend.userservice.user.api.dto.AddressCreateRequestDto;
import com.swiftapp_backend.userservice.user.api.dto.AddressCreateResponseDto;
import com.swiftapp_backend.userservice.user.domain.model.AddressModel;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {
    public AddressModel toModel(AddressCreateRequestDto dto) {
        return AddressModel
                .builder()
                .fullAddress(dto.getFullAddress())
                .defaultAddress(dto.getDefaultAddress())
                .isFavouritePlace(dto.getIsFavouritePlace())
                .country(dto.getCountry())
                .addressType(dto.getAddressType())
                .label(dto.getLabel())
                .state(dto.getState())
                .city(dto.getCity())
                .landmark(dto.getLandmark())
                .longitude(dto.getLongitude())
                .latitude(dto.getLatitude())
                .pinCode(dto.getPinCode())
                .active(true)
                .build();
    }

    public AddressCreateResponseDto toResponseDto(AddressModel model) {
        return AddressCreateResponseDto
                .builder()
                .id(model.getId())
                .isFavouritePlace(model.getIsFavouritePlace())
                .city(model.getCity())
                .landmark(model.getLandmark())
                .defaultAddress(model.getDefaultAddress())
                .country(model.getCountry())
                .label(model.getLabel())
                .addressType(model.getAddressType())
                .latitude(model.getLatitude())
                .longitude(model.getLongitude())
                .pinCode(model.getPinCode())
                .state(model.getState())
                .fullAddress(model.getFullAddress())
                .build();
    }
}
