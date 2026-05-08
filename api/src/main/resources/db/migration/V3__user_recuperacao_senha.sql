-- =============================================================
-- V3 - Tokens de recuperacao de senha (esqueci minha senha)
-- =============================================================
ALTER TABLE users
    ADD COLUMN token_recuperacao             VARCHAR(100) NULL,
    ADD COLUMN token_recuperacao_expira_em   DATETIME     NULL;
