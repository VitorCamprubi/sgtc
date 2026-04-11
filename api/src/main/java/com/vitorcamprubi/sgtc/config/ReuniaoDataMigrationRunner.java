package com.vitorcamprubi.sgtc.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(3)
public class ReuniaoDataMigrationRunner implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    public ReuniaoDataMigrationRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        try {
            jdbc.execute("ALTER TABLE reunioes MODIFY COLUMN atividades_realizadas TEXT");
        } catch (DataAccessException ignored) {
            // Non-MySQL dialects (e.g., H2 tests) may reject this syntax.
        }

        try {
            jdbc.execute("ALTER TABLE reunioes MODIFY COLUMN relatorio TEXT");
        } catch (DataAccessException ignored) {
            // Non-MySQL dialects (e.g., H2 tests) may reject this syntax.
        }
    }
}
