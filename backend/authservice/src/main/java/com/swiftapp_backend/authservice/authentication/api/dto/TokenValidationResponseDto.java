package com.swiftapp_backend.authservice.authentication.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenValidationResponseDto {
    private boolean valid;
    private String email;
    private Long userId;
}
