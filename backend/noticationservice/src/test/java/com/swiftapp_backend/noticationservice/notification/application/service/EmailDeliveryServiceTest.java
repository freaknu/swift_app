package com.swiftapp_backend.noticationservice.notification.application.service;

import com.swiftapp_backend.noticationservice.notification.domain.enums.UserRole;
import com.swiftapp_backend.noticationservice.notification.domain.repository.EmailDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class EmailDeliveryServiceTest {
    @Autowired private EmailDeliveryService service;
    @Autowired private EmailDeliveryRepository repository;

    @BeforeEach
    void clear() {
        repository.deleteAll();
    }

    @Test
    void aliasesSendOnlyOneEmailForTheSameRideAndRecipient() {
        AtomicInteger sends = new AtomicInteger();
        service.deliver("DRIVER_ASSIGNED:17", 31L, UserRole.RIDER, sends::incrementAndGet);
        service.deliver("DRIVER_ASSIGNED:17", 31L, UserRole.RIDER, sends::incrementAndGet);
        assertThat(sends).hasValue(1);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void failedSendRollsBackMarkerAndKafkaRetryCanResend() {
        assertThatThrownBy(() -> service.deliver("CANCELLED:18", 31L, UserRole.RIDER,
                () -> { throw new IllegalStateException("SMTP unavailable"); }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(repository.count()).isZero();
        AtomicInteger sends = new AtomicInteger();
        service.deliver("CANCELLED:18", 31L, UserRole.RIDER, sends::incrementAndGet);
        assertThat(sends).hasValue(1);
        assertThat(repository.count()).isEqualTo(1);
    }
}
