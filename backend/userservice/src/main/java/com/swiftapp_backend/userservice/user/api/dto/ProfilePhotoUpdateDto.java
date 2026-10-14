package com.swiftapp_backend.userservice.user.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfilePhotoUpdateDto {
    private String profileImageUrl;
}
