package com.swiftapp_backend.paymentservice.payment;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/** Same fault/concurrency regressions against an explicitly supplied disposable PostgreSQL database. */
@EnabledIfSystemProperty(named = "swift.qa.postgres.url", matches = "jdbc:postgresql:.*")
class PaymentProviderPostgresTest extends PaymentProviderConcurrencyTest {
    @DynamicPropertySource
    static void disposablePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getProperty("swift.qa.postgres.url"));
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.datasource.username", () -> System.getProperty("swift.qa.postgres.username"));
        registry.add("spring.datasource.password", () -> System.getProperty("swift.qa.postgres.password"));
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }
}
