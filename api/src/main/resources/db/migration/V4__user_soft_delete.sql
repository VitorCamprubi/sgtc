-- =============================================================
-- V4 - Soft delete de usuario
-- =============================================================
-- Adiciona a coluna `ativo` em users. Usuarios "excluidos" pelo admin
-- passam a ser marcados como ativo=false em vez de removidos fisicamente.
-- Isso preserva o historico (comentarios, grupos arquivados, reunioes)
-- e atende auditoria.
ALTER TABLE users
    ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;
