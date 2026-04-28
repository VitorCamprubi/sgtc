package com.vitorcamprubi.sgtc.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(4)
public class GrupoStatusMigrationRunner implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    public GrupoStatusMigrationRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        try {
            jdbc.execute("ALTER TABLE grupos ADD COLUMN status VARCHAR(16)");
        } catch (DataAccessException ignored) {
            // Column may already exist.
        }

        try {
            jdbc.execute("ALTER TABLE grupos ADD COLUMN nota_final DECIMAL(4,2)");
        } catch (DataAccessException ignored) {
            // Column may already exist.
        }

        try {
            jdbc.execute("ALTER TABLE grupos ADD COLUMN arquivado_em DATETIME");
        } catch (DataAccessException ignored) {
            // Column may already exist.
        }

        jdbc.update("UPDATE grupos SET status = 'EM_CURSO' WHERE status IS NULL OR status NOT IN ('EM_CURSO', 'APROVADO', 'REPROVADO')");

        try {
            jdbc.execute("ALTER TABLE grupos MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'EM_CURSO'");
        } catch (DataAccessException ignored) {
            // Non-MySQL dialects may reject this syntax.
        }

        try {
            jdbc.execute("ALTER TABLE grupo_aluno ADD COLUMN status VARCHAR(16)");
        } catch (DataAccessException ignored) {
            // Column may already exist.
        }

        jdbc.update("UPDATE grupo_aluno SET status = 'EM_CURSO' WHERE status IS NULL OR status NOT IN ('EM_CURSO', 'APROVADO', 'REPROVADO')");

        try {
            jdbc.execute("ALTER TABLE grupo_aluno MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'EM_CURSO'");
        } catch (DataAccessException ignored) {
            // Non-MySQL dialects may reject this syntax.
        }
    }
}
