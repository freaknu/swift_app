package com.swiftapp_backend.authservice.authentication.api.dto;

import com.swiftapp_backend.authservice.authentication.domain.enums.AppUserRoles;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SocialLoginRequestDto {
    private String idToken;
    private String provider; // "GOOGLE" or "APPLE"
    private String email;
    private String name;
    private AppUserRoles role;
}
