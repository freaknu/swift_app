package com.swiftapp_backend.chatservice.chat.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Removes the original rider-only uniqueness rule. A rider can have many
 * completed rides and therefore many historical chat sessions; the ride is
 * the idempotency key, not the rider.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatSchemaUpgrade implements org.springframework.beans.factory.SmartInitializingSingleton {
    private final DataSource dataSource;

    @Override
    public void afterSingletonsInstantiated() {
        try (Connection connection = dataSource.getConnection()) {
            if (!"PostgreSQL".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName())) {
                return;
            }
            dropRiderUniqueConstraints(connection);
        } catch (SQLException ex) {
            throw new IllegalStateException("Could not upgrade chat session constraints", ex);
        }
    }

    private void dropRiderUniqueConstraints(Connection connection) throws SQLException {
        List<String> names = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement("""
                SELECT c.conname
                FROM pg_constraint c
                JOIN pg_class t ON t.oid = c.conrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE n.nspname = current_schema()
                  AND t.relname = 'chat_session'
                  AND c.contype = 'u'
                  AND pg_get_constraintdef(c.oid) ILIKE '%rider_id%'
                """)) {
            try (ResultSet result = query.executeQuery()) {
                while (result.next()) names.add(result.getString(1));
            }
        }
        try (Statement statement = connection.createStatement()) {
            for (String name : names) {
                statement.execute("ALTER TABLE chat_session DROP CONSTRAINT \""
                        + name.replace("\"", "\"\"") + "\"");
                log.info("Removed obsolete chat rider uniqueness constraint {}", name);
            }
        }
    }
}
