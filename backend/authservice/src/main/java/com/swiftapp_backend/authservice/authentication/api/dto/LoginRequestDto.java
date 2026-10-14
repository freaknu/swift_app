package com.swiftapp_backend.authservice.authentication.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class LoginRequestDto {
    private String email;
    private int otp;
}
