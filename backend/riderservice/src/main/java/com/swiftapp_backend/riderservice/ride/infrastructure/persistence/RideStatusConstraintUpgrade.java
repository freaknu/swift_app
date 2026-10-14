package com.swiftapp_backend.riderservice.ride.infrastructure.persistence;

import com.swiftapp_backend.riderservice.ride.domain.enums.RideStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Hibernate's schema update does not reliably replace older enum CHECK
 * constraints. Upgrade only the two known ride status constraints before
 * Kafka listeners start, so MATCH_FAILED can be persisted on existing DBs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RideStatusConstraintUpgrade implements SmartInitializingSingleton {
    private final DataSource dataSource;

    @Override
    public void afterSingletonsInstantiated() {
        try (Connection connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
                return;
            }
            upgrade(connection, "rides", "current_status", "rides_current_status_check");
            upgrade(connection, "ride_status_history", "ride_status",
                    "ride_status_history_ride_status_check");
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not upgrade ride status constraints", ex);
        }
    }

    private void upgrade(Connection connection, String table, String column, String constraint)
            throws SQLException {
        String definition;
        try (PreparedStatement query = connection.prepareStatement("""
                SELECT pg_get_constraintdef(c.oid)
                FROM pg_constraint c
                JOIN pg_class t ON t.oid = c.conrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE n.nspname = current_schema() AND t.relname = ?
                  AND c.conname = ? AND c.contype = 'c'
                """)) {
            query.setString(1, table);
            query.setString(2, constraint);
            try (ResultSet result = query.executeQuery()) {
                if (!result.next()) {
                    return;
                }
                definition = result.getString(1);
            }
        }
        if (definition != null && definition.contains("MATCH_FAILED")) {
            return;
        }

        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + table + " DROP CONSTRAINT " + constraint);
            statement.execute("ALTER TABLE " + table + " ADD CONSTRAINT " + constraint
                    + " CHECK (" + column + " IN (" + allowedStatuses() + "))");
            connection.commit();
            log.info("Upgraded {} to accept MATCH_FAILED", constraint);
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private String allowedStatuses() {
        return Arrays.stream(RideStatus.values())
                .map(status -> "'" + status.name() + "'")
                .collect(Collectors.joining(", "));
    }
}
