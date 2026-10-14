package com.swiftapp_backend.authservice.authentication.api.dto;

import lombok.Data;

@Data
public class HomeReadyRequestDto {
    private String otpCompletionToken;
}
