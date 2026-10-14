package com.swiftapp_backend.paymentservice.payment.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.Connection;

/** Follows this repository's additive startup upgrade convention; preserves queued events. */
@Component
@RequiredArgsConstructor
public class PaymentOutboxSchemaUpgrade implements SmartInitializingSingleton {
    private final DataSource dataSource;

    @Override
    public void afterSingletonsInstantiated() {
        try (Connection connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) return;
            boolean previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/upgrades/20261014_outbox_delivery.sql"));
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not upgrade outbox delivery metadata", ex);
        }
    }
}
