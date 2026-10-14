package com.swiftapp_backend.userservice.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class UserCreateRequestDto {
    private Long id;
    private Long authUserId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String profileImageUrl;
}
