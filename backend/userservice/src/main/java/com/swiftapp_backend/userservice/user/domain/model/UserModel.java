package com.swiftapp_backend.userservice.user.domain.model;

import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserModel {

    private Long id;

    // ID from Auth Service
    private Long authUserId;

    private String fullName;

    private String email;

    private String phoneNumber;

    private String profileImageUrl;

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Boolean deleted = false;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private UUID walletId;
    @Builder.Default
    private List<AddressModel> addresses = new ArrayList<>();

    @Builder.Default
    private List<EmergencyContactModel> emergencyContacts = new ArrayList<>();
}
