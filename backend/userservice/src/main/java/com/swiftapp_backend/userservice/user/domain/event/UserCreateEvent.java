package com.swiftapp_backend.userservice.user.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@RequiredArgsConstructor
public class UserCreateEvent {
    private Long authUserId;
    private String email;
    private String name;
    private String phoneNumber;
}
