-- =============================================================
-- V2 - Adiciona expiracao no token de confirmacao de reuniao
-- =============================================================
ALTER TABLE reunioes
    ADD COLUMN token_expira_em DATETIME NULL AFTER token_confirmacao;
