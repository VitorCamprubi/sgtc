package com.vitorcamprubi.sgtc.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class RoleDataMigrationRunner implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    public RoleDataMigrationRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        // If the old schema used ENUM, convert to VARCHAR first to avoid startup failures
        // when unknown enum values are still present.
        try {
            jdbc.execute("ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL");
        } catch (DataAccessException ignored) {
            // Non-MySQL dialects (e.g., H2 tests) may reject this syntax.
        }

        jdbc.update("UPDATE users SET role = 'PROFESSOR' WHERE role NOT IN ('ADMIN', 'ALUNO', 'PROFESSOR')");

        Long professorEmailCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = 'professor@sgtc.local'",
                Long.class
        );

        if (professorEmailCount != null && professorEmailCount == 0L) {
            jdbc.update("UPDATE users SET email = 'professor@sgtc.local' WHERE email = 'orientador@sgtc.local'");
        }
    }
}
