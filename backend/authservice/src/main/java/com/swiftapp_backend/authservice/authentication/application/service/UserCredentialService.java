package com.swiftapp_backend.authservice.authentication.application.service;

import com.swiftapp_backend.authservice.authentication.api.dto.UserCredentialDto;
import com.swiftapp_backend.authservice.authentication.domain.model.UserCredentials;
import com.swiftapp_backend.authservice.authentication.domain.repository.UserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCredentialService {
    private final UserCredentialRepository userCredentialRepository;

    public List<UserCredentialDto> getAllCredentials(Long userId) {
        List<UserCredentials> allUserCredentials = userCredentialRepository.findAllByUserId(userId);
        return allUserCredentials
                .stream()
                .map(cred->{
                    return UserCredentialDto
                            .builder()
                            .id(cred.getId())
                            .userId(cred.getUserId())
                            .os(cred.getOs())
                            .deviceModel(cred.getDeviceModel())
                            .loggedInAt(cred.getLoggedInAt())
                            .deviceName(cred.getDeviceName())
                            .isLoggedOut(cred.isLoggedOut())
                            .build();
                })
                .toList();
    }
}
