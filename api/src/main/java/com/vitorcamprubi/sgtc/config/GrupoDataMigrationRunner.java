package com.vitorcamprubi.sgtc.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class GrupoDataMigrationRunner implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    public GrupoDataMigrationRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        try {
            jdbc.execute("ALTER TABLE grupos ADD COLUMN materia VARCHAR(3)");
        } catch (DataAccessException ignored) {
            // Column may already exist.
        }

        try {
            // Legacy schemas may still have professor/coprofessor columns.
            jdbc.execute("UPDATE grupos SET orientador_id = professor_id WHERE orientador_id IS NULL AND professor_id IS NOT NULL");
        } catch (DataAccessException ignored) {
            // Legacy column may not exist.
        }

        try {
            jdbc.execute("UPDATE grupos SET coorientador_id = coprofessor_id WHERE coorientador_id IS NULL AND coprofessor_id IS NOT NULL");
        } catch (DataAccessException ignored) {
            // Legacy column may not exist.
        }

        jdbc.update("UPDATE grupos SET coorientador_id = NULL WHERE coorientador_id = orientador_id");
        jdbc.update("UPDATE grupos SET materia = 'TG' WHERE materia IS NULL OR materia NOT IN ('TG', 'PTG')");

        try {
            jdbc.execute("ALTER TABLE grupos MODIFY COLUMN materia VARCHAR(3) NOT NULL DEFAULT 'TG'");
        } catch (DataAccessException ignored) {
            // Non-MySQL dialects can reject this syntax.
        }
    }
}
