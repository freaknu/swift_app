package com.swiftapp_backend.authservice.authentication.api.dto;

import com.swiftapp_backend.authservice.authentication.domain.enums.AppUserRoles;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class CreateAccountRequestDto {
    private String email;
    private String name;
    private String phoneNumber;
    private AppUserRoles role;
}
