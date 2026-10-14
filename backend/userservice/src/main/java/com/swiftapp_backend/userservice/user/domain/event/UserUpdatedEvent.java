package com.swiftapp_backend.userservice.user.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdatedEvent {
    private Long authUserId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String profileImageUrl;
    private LocalDateTime updatedAt;
}
