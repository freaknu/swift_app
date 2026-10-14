package com.swiftapp_backend.authservice.authentication.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDeletedEvent {
    private Long authUserId;
    private String email;
}
