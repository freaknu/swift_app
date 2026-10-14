package com.swiftapp_backend.userservice.user.infrastructure.persistence.mapper;

import com.swiftapp_backend.userservice.user.domain.enums.AddressType;
import com.swiftapp_backend.userservice.user.domain.model.UserModel;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.AddressEntity;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.EmergencyContactEntity;
import com.swiftapp_backend.userservice.user.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class UserPersistenceMapperTest {
    private final UserPersistenceMapper mapper = new UserPersistenceMapper(
            new AddressPersistenceMapper(),
            new EmergencyContactPersistenceMapper()
    );

    @Test
    void preservesChildrenAndRebuildsBackReferencesInBothDirections() {
        UserEntity entity = UserEntity.builder()
                .id(10L)
                .authUserId(20L)
                .fullName("Test User")
                .email("test@example.com")
                .phoneNumber("1234567890")
                .active(true)
                .deleted(false)
                .build();
        AddressEntity address = AddressEntity.builder()
                .id(30L)
                .addressType(AddressType.HOME)
                .label("Home")
                .fullAddress("1 Main Street")
                .city("City")
                .state("State")
                .country("Country")
                .pinCode("100001")
                .latitude(1.0)
                .longitude(2.0)
                .active(true)
                .user(entity)
                .build();
        EmergencyContactEntity contact = EmergencyContactEntity.builder()
                .id(40L)
                .contactType("Family")
                .contactPersonName("Contact")
                .contactPersonNumber("9999999999")
                .isActive(true)
                .user(entity)
                .build();
        entity.setAddresses(new ArrayList<>(List.of(address)));
        entity.setEmergencyContacts(new ArrayList<>(List.of(contact)));

        UserModel domain = mapper.toDomain(entity);

        assertEquals(1, domain.getAddresses().size());
        assertEquals(1, domain.getEmergencyContacts().size());
        assertSame(domain, domain.getAddresses().getFirst().getUser());
        assertSame(domain, domain.getEmergencyContacts().getFirst().getUser());

        UserEntity roundTrip = mapper.toEntity(domain);

        assertEquals(1, roundTrip.getAddresses().size());
        assertEquals(1, roundTrip.getEmergencyContacts().size());
        assertSame(roundTrip, roundTrip.getAddresses().getFirst().getUser());
        assertSame(roundTrip, roundTrip.getEmergencyContacts().getFirst().getUser());
    }
}
