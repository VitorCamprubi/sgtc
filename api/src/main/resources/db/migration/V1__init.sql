-- =============================================================
-- V1 - Schema inicial consolidado do SGTC
-- =============================================================

-- ----------------------------- USERS -----------------------------
CREATE TABLE users (
    id                              BIGINT       NOT NULL AUTO_INCREMENT,
    nome                            VARCHAR(255) NOT NULL,
    email                           VARCHAR(255) NOT NULL,
    senha_hash                      VARCHAR(255) NOT NULL,
    role                            VARCHAR(20)  NOT NULL,
    ra                              VARCHAR(255) NULL,
    email_confirmado                BOOLEAN      NOT NULL DEFAULT FALSE,
    token_confirmacao               VARCHAR(100) NULL,
    token_confirmacao_expira_em     DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_ra (ra)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------- GRUPOS ----------------------------
CREATE TABLE grupos (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    titulo            VARCHAR(255) NOT NULL,
    orientador_id     BIGINT       NOT NULL,
    coorientador_id   BIGINT       NULL,
    materia           VARCHAR(3)   NOT NULL DEFAULT 'TG',
    status            VARCHAR(16)  NOT NULL DEFAULT 'EM_CURSO',
    nota_final        DOUBLE       NULL,
    arquivado_em      DATETIME     NULL,
    created_at        DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_grupos_orientador
        FOREIGN KEY (orientador_id) REFERENCES users(id),
    CONSTRAINT fk_grupos_coorientador
        FOREIGN KEY (coorientador_id) REFERENCES users(id),
    KEY idx_grupos_orientador (orientador_id),
    KEY idx_grupos_coorientador (coorientador_id),
    KEY idx_grupos_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------- GRUPO_ALUNO -------------------------
CREATE TABLE grupo_aluno (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    grupo_id  BIGINT      NOT NULL,
    aluno_id  BIGINT      NOT NULL,
    status    VARCHAR(16) NOT NULL DEFAULT 'EM_CURSO',
    PRIMARY KEY (id),
    UNIQUE KEY uk_grupo_aluno_grupo_aluno (grupo_id, aluno_id),
    CONSTRAINT fk_grupo_aluno_grupo
        FOREIGN KEY (grupo_id) REFERENCES grupos(id) ON DELETE CASCADE,
    CONSTRAINT fk_grupo_aluno_aluno
        FOREIGN KEY (aluno_id) REFERENCES users(id),
    KEY idx_grupo_aluno_aluno (aluno_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------- REUNIOES --------------------------
CREATE TABLE reunioes (
    id                            BIGINT       NOT NULL AUTO_INCREMENT,
    grupo_id                      BIGINT       NOT NULL,
    data_hora                     DATETIME     NOT NULL,
    pauta                         VARCHAR(255) NOT NULL,
    observacoes                   VARCHAR(255) NULL,
    status                        VARCHAR(40)  NULL,
    relatorio                     TEXT         NULL,
    encerrada_em                  DATETIME     NULL,
    numero_encontro               INT          NULL,
    data_atividades_realizadas    DATE         NULL,
    atividades_realizadas         TEXT         NULL,
    desempenho_grupo              VARCHAR(20)  NULL,
    professor_disciplina          VARCHAR(255) NULL,
    orientador_assinatura         VARCHAR(255) NULL,
    coorientador_assinatura       VARCHAR(255) NULL,
    criado_por                    BIGINT       NOT NULL,
    created_at                    DATETIME     NOT NULL,
    confirmada_pelo_professor     BOOLEAN      NULL,
    token_confirmacao             VARCHAR(100) NULL,
    respondida_em                 DATETIME     NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_reunioes_grupo
        FOREIGN KEY (grupo_id) REFERENCES grupos(id) ON DELETE CASCADE,
    CONSTRAINT fk_reunioes_criado_por
        FOREIGN KEY (criado_por) REFERENCES users(id),
    KEY idx_reunioes_grupo (grupo_id),
    KEY idx_reunioes_status (status),
    KEY idx_reunioes_token (token_confirmacao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------- DOCUMENTOS_VERSAO ---------------------
CREATE TABLE documentos_versao (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    grupo_id     BIGINT       NOT NULL,
    titulo       VARCHAR(255) NOT NULL,
    versao       INT          NOT NULL,
    file_path    VARCHAR(255) NOT NULL,
    mime_type    VARCHAR(255) NOT NULL,
    tamanho      BIGINT       NOT NULL,
    enviado_por  BIGINT       NOT NULL,
    created_at   DATETIME     NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_doc_grupo
        FOREIGN KEY (grupo_id) REFERENCES grupos(id) ON DELETE CASCADE,
    CONSTRAINT fk_doc_enviado_por
        FOREIGN KEY (enviado_por) REFERENCES users(id),
    KEY idx_doc_grupo_versao (grupo_id, versao),
    KEY idx_doc_enviado_por (enviado_por)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------- DOCUMENTOS_COMENTARIOS ----------------
CREATE TABLE documentos_comentarios (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    documento_id  BIGINT        NOT NULL,
    autor_id      BIGINT        NOT NULL,
    texto         VARCHAR(4000) NOT NULL,
    created_at    DATETIME      NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_comentario_documento
        FOREIGN KEY (documento_id) REFERENCES documentos_versao(id) ON DELETE CASCADE,
    CONSTRAINT fk_comentario_autor
        FOREIGN KEY (autor_id) REFERENCES users(id),
    KEY idx_comentario_documento (documento_id),
    KEY idx_comentario_autor (autor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
