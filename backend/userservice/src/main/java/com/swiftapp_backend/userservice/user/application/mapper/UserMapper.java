package com.swiftapp_backend.userservice.user.application.mapper;

import com.swiftapp_backend.userservice.user.api.dto.UserCreateRequestDto;
import com.swiftapp_backend.userservice.user.api.dto.UserResponseDto;
import com.swiftapp_backend.userservice.user.domain.model.UserModel;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserModel toModel(UserCreateRequestDto dto) {
        return UserModel
                .builder()
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .authUserId(dto.getAuthUserId())
                .phoneNumber(dto.getPhoneNumber())
                .profileImageUrl(dto.getProfileImageUrl())
                .build();
    }


    public UserResponseDto toResponseDto(UserModel model) {
        return UserResponseDto
                .builder()
                .id(model.getId())
                .email(model.getEmail())
                .fullName(model.getFullName())
                .authUserId(model.getAuthUserId())
                .active(model.getActive())
                .deleted(model.getDeleted())
                .phoneNumber(model.getPhoneNumber())
                .profileImageUrl(model.getProfileImageUrl())
                .createdAt(model.getCreatedAt())
                .updatedAt(model.getUpdatedAt())
                .build();
    }
}
