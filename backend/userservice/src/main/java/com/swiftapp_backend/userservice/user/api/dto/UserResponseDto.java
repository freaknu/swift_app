package com.swiftapp_backend.userservice.user.api.dto;

import com.swiftapp_backend.userservice.user.api.dto.AddressCreateResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class UserResponseDto {
    private Long id;
    private Long authUserId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String profileImageUrl;
    private Boolean active = true;
    private Boolean deleted = false;
    private List<Long> appliedOfferIds;
    private List<AddressCreateResponseDto> allAddresses;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}