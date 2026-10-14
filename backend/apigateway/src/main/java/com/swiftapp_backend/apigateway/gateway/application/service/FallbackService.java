package com.swiftapp_backend.apigateway.gateway.application.service;

import org.springframework.stereotype.Service;

@Service
public class FallbackService {
    public String unavailableMessage(String displayName) {
        return displayName + " is currently unavailable";
    }
}
