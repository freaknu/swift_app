package com.swiftapp_backend.userservice.user.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Preserve existing data while allowing social accounts and shared emergency contacts. */
@Component
@RequiredArgsConstructor
public class UserSchemaUpgrade implements SmartInitializingSingleton {
    private final DataSource dataSource;
    @Override public void afterSingletonsInstantiated() {
        try (Connection c = dataSource.getConnection()) {
            if (!"PostgreSQL".equalsIgnoreCase(c.getMetaData().getDatabaseProductName())) return;
            try (Statement s = c.createStatement()) {
                s.execute("ALTER TABLE user_details ALTER COLUMN phone_number DROP NOT NULL");
                s.execute("UPDATE user_details SET phone_number = NULL WHERE trim(phone_number) = ''");
            }
            List<String> names = new ArrayList<>();
            try (PreparedStatement s = c.prepareStatement("""
                    SELECT c.conname FROM pg_constraint c
                    JOIN pg_class t ON t.oid = c.conrelid
                    JOIN pg_namespace n ON n.oid = t.relnamespace
                    WHERE n.nspname = current_schema() AND t.relname = 'emergency_contact'
                      AND c.contype = 'u' AND array_length(c.conkey, 1) = 1
                      AND EXISTS (SELECT 1 FROM pg_attribute a
                          WHERE a.attrelid = t.oid AND a.attnum = ANY(c.conkey)
                            AND a.attname IN ('contact_type','contact_person_name','contact_person_number'))
                    """)) {
                try (ResultSet r = s.executeQuery()) { while (r.next()) names.add(r.getString(1)); }
            }
            try (Statement s = c.createStatement()) {
                for (String name : names) s.execute("ALTER TABLE emergency_contact DROP CONSTRAINT \""
                        + name.replace("\"", "\"\"") + "\"");
            }
        } catch (SQLException e) { throw new IllegalStateException("Could not upgrade user profile schema", e); }
    }
}
