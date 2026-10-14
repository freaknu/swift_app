package com.swiftapp_backend.apigateway;

import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ApigatewayApplicationTests {
	@Autowired
	private TimeLimiterRegistry timeLimiterRegistry;

	@Test
	void contextLoads() {
	}

	@Test
	void riderCircuitBreakerAllowsColdBookingRequestsToFinish() {
		assertThat(timeLimiterRegistry.timeLimiter("riderServiceCircuitBreaker")
				.getTimeLimiterConfig().getTimeoutDuration())
				.isEqualTo(Duration.ofSeconds(10));
	}

}
