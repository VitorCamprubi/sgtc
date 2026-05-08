# SGTC — Sistema de Gerenciamento de Trabalhos de Graduação

Sistema completo para acompanhamento de TCC com:
- API em **Java 21 + Spring Boot 3.4** (`api/`)
- Frontend em **Angular 20** standalone components (`web/`)
- Banco **MySQL 8** com schema versionado por **Flyway**
- Notificações por e-mail (cadastro, reunião, comentário, nota final)
- Geração de PDF do relatório de reuniões a partir de template (PDFBox)
- Stack completa via `docker-compose.yml`

## Sumário

- [Arquitetura](#arquitetura)
- [Pré-requisitos](#pré-requisitos)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Rodando em desenvolvimento](#rodando-em-desenvolvimento)
- [Rodando em produção](#rodando-em-produção)
- [Primeiro admin em produção](#primeiro-admin-em-produção)
- [Migrations (Flyway)](#migrations-flyway)
- [Endpoints principais](#endpoints-principais)
- [Backup e restore](#backup-e-restore)
- [Troubleshooting](#troubleshooting)

---

## Arquitetura

| Camada    | Stack                                           | Porta padrão |
|-----------|-------------------------------------------------|--------------|
| Frontend  | Angular 20 + nginx                              | 4200         |
| Backend   | Spring Boot 3.4 + JWT + Flyway                  | 8080         |
| Database  | MySQL 8                                         | 3307         |
| Adminer   | (perfil `tools`, opcional)                      | 8081         |

Papéis: `ADMIN`, `PROFESSOR`, `ALUNO`. Matérias: `TG`, `PTG`. Status de grupo: `EM_CURSO`, `APROVADO`, `REPROVADO` (≥6 = aprovado).

---

## Pré-requisitos

**Para rodar com Docker (recomendado):**
- Docker Desktop ou Docker Engine + Compose v2

**Para rodar nativo (sem Docker):**
- Java 21+
- Maven 3.9+ (ou wrapper `mvnw`/`mvnw.cmd`)
- Node.js 22+ e npm 10+
- MySQL 8+ em `localhost:3307`

---

## Variáveis de ambiente

Copie o template e edite:

```bash
cp .env.example .env
```

Campos obrigatórios para **prod**:
- `SPRING_PROFILES_ACTIVE=prod`
- `MYSQL_USER`, `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`
- `JWT_SECRET` — gere com `openssl rand -base64 64`. Em prod a aplicação **se recusa a subir** se for o valor de dev ou tiver menos de 32 bytes.
- `APP_CORS_ALLOWED_ORIGINS` — domínio público do front (`https://app.seudominio.com`).
- `APP_API_URL` / `APP_WEB_URL` — usados nos links dos e-mails.
- `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`.

---

## Rodando em desenvolvimento

### Opção A — Docker (mais simples)

```bash
cp .env.example .env       # ajuste se quiser
docker compose up -d --build
```

URLs:
- Front: <http://localhost:4200>
- API:   <http://localhost:8080>
- MySQL: `localhost:3307`

Usuários seed (criados pelo `DataLoader`, **só em dev**):
- ADMIN: `admin@sgtc.local` / `admin123`
- PROFESSOR: `professor@sgtc.local` / `prof123`
- ALUNO: `aluno@sgtc.local` / `aluno123`

Para subir o Adminer junto:
```bash
docker compose --profile tools up -d
# Adminer em http://localhost:8081
```

### Opção B — Nativo

```bash
# 1) Banco (precisa de um MySQL 8 rodando em localhost:3307 com user 'sgtc' / db 'sgtc')

# 2) Backend
cd api
./mvnw spring-boot:run             # Linux/macOS
# mvnw.cmd spring-boot:run         # Windows

# 3) Frontend (em outro terminal)
cd web
npm install
npm start
```

O Angular dev server faz proxy de `/api/**` e `/public/**` para `http://localhost:8080` via `web/proxy.conf.json`.

---

## Rodando em produção

1. **Edite `.env`** com valores reais (sem usar nenhum default sugerido). Confirme:
   - `SPRING_PROFILES_ACTIVE=prod`
   - `JWT_SECRET` aleatório de 64+ bytes (`openssl rand -base64 64`)
   - Senha forte do MySQL
   - SMTP configurado (Gmail App Password / SES / SendGrid)
   - `APP_*_URL` apontando para os domínios públicos (com `https://`)
   - `APP_CORS_ALLOWED_ORIGINS` com o domínio do front
   - `SGTC_DOMAIN` com o domínio público (ex.: `sgtc.fatectatui.edu.br`)
2. **Suba a stack com TLS automático:**
   ```bash
   docker compose --profile https up -d --build
   ```
   O serviço `caddy` levanta na porta 443, gera certificado Let's Encrypt automaticamente e faz proxy reverso para o backend e o frontend. As portas 4200/8080 do frontend/backend não precisam ficar expostas — em prod, retire-as do compose ou bloqueie no firewall.
3. **Crie o primeiro admin** (próxima seção).
4. **Configure o backup automático** (seção [Backup e restore](#backup-e-restore)).

A imagem `backend` roda como **usuário não-root**, em `eclipse-temurin:21-jre-jammy`. Os uploads ficam num volume Docker (`sgtc_uploads`) montado em `/var/sgtc/uploads`.

### Segurança em produção

- **Rate limiting** no `POST /api/auth/login`: 5 tentativas por minuto por IP (Bucket4j em memória). Excedeu → `429 Too Many Requests` com `Retry-After`.
- **Política de senha**: 8+ caracteres com letras e números (validado tanto na criação pelo admin quanto no fluxo de redefinição).
- **HSTS, CSP, X-Frame-Options** aplicados pelo Caddy (ver `Caddyfile`).

---

## Primeiro admin em produção

O `DataLoader` **não roda em prod**. Crie o admin manualmente:

```bash
# 1) Gere o BCrypt da senha. Mais simples: suba uma vez em DEV,
#    crie o admin pela API, e copie o senha_hash da tabela users.

# 2) Insira no MySQL de prod:
docker compose exec mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" sgtc <<'SQL'
INSERT INTO users (nome, email, senha_hash, role, email_confirmado)
VALUES ('Admin', 'admin@seudominio.com',
        '<bcrypt-aqui>', 'ADMIN', TRUE);
SQL
```

A partir daí, faça login pela UI e cadastre os demais usuários por `/admin/usuarios`.

---

## Migrations (Flyway)

Schema é gerenciado por Flyway. Hibernate apenas valida (`ddl-auto=validate`).

- **Localização:** `api/src/main/resources/db/migration/`
- **Convenção:** `V<n>__descricao.sql` (ex.: `V3__add_user_telefone.sql`)
- **Toda mudança de entidade** exige uma nova migration. Nunca edite uma `Vn` que já rodou em algum ambiente.

Estado atual:
- `V1__init.sql` — schema completo (users, grupos, grupo_aluno, reunioes, documentos_versao, documentos_comentarios)
- `V2__reuniao_token_ttl.sql` — adiciona `token_expira_em` em `reunioes`
- `V3__user_recuperacao_senha.sql` — adiciona colunas de token de recuperação de senha em `users`

---

## Endpoints principais

### Auth (público)
- `POST /api/auth/login` — `{ email, senha }` → JWT (com rate limit de 5/min/IP)
- `GET  /api/auth/verify-email?token=...` — confirma e-mail
- `POST /api/auth/resend-verification` — reenvia link
- `POST /api/auth/forgot-password` — `{ email }` → envia link de recuperação (válido por 1h)
- `POST /api/auth/reset-password` — `{ token, novaSenha }` → redefine senha
- `GET  /api/auth/reunioes/confirmar?token=...` — professor confirma reunião pelo e-mail
- `GET  /api/auth/reunioes/recusar?token=...` — professor recusa reunião

### Auth (logado)
- `GET /api/auth/me` — perfil do usuário logado

### Admin (`hasRole('ADMIN')`)
- `GET/POST/PUT/DELETE /api/admin/usuarios`

### Grupos
- `POST/PUT/DELETE /api/grupos` — ADMIN
- `GET /api/grupos/me` — grupos do usuário logado (em curso)
- `GET /api/grupos/me/arquivos[/aprovados|/reprovados]` — arquivados (PROFESSOR/ADMIN)
- `GET /api/grupos/{id}` — detalhe
- `POST/PUT/DELETE /api/grupos/{id}/membros` — ADMIN
- `POST /api/grupos/{id}/nota-final` — ADMIN

### Documentos
- `POST /api/grupos/{grupoId}/documentos` — upload (PDF, DOC, DOCX, ODT, RTF, TXT)
- `GET /api/grupos/{grupoId}/documentos`
- `GET /api/documentos/{docId}/download`
- `PUT /api/documentos/{docId}` — atualizar título
- `DELETE /api/documentos/{docId}`

### Comentários
- `GET /api/documentos/{docId}/comentarios`
- `POST /api/documentos/{docId}/comentarios` — orientador/coorientador/admin
- `PUT/DELETE /api/comentarios/{id}` — autor ou admin

### Reuniões
- `GET /api/grupos/{grupoId}/reunioes`
- `POST /api/grupos/{grupoId}/reunioes` — agenda (envia e-mail ao professor com link de confirmar/recusar)
- `PUT /api/reunioes/{id}` — remarca
- `POST /api/reunioes/{id}/concluir`
- `POST /api/reunioes/{id}/cancelar`
- `GET /api/grupos/{grupoId}/reunioes/pdf` — PDF do relatório (até 6 encontros)

### Debug (somente perfil dev, ADMIN)
- `GET /api/debug/users` — em prod retorna 404

---

## Backup e restore

Há scripts prontos em `scripts/`:

```bash
# Backup imediato (gera backups/db-<timestamp>.sql.gz e backups/uploads-<timestamp>.tar.gz)
./scripts/backup.sh

# Restaurar a partir de um par de arquivos (timestamp visto no filename)
./scripts/restore.sh 20260506-020000
```

Variáveis principais (com defaults sensatos):
- `BACKUP_DIR` — diretório de saída (default `./backups`)
- `RETENTION_DAYS` — backups com mais de N dias são apagados (default `14`)

### Cron de produção

Em `/etc/crontab` ou no usuário que roda o stack:

```cron
# todo dia às 02:00 da manhã
0 2 * * * cd /opt/sgtc && ./scripts/backup.sh >> /var/log/sgtc-backup.log 2>&1
```

Recomendado também copiar a pasta `backups/` para fora do servidor (S3, Backblaze B2, rclone para Drive/OneDrive, etc.) para sobreviver a um disco perdido.

---

## Troubleshooting

| Sintoma | Causa provável | Solução |
|---------|----------------|---------|
| `IllegalStateException: JWT_SECRET ...` no boot em prod | Secret não setado, ou é o de dev, ou < 32 bytes | Gere com `openssl rand -base64 64` e setar `JWT_SECRET` |
| `ValidationException` no Hibernate ao subir | Schema diverge das entities | Rodou alguma migration faltando? Confira `flyway_schema_history` |
| 401 em todas as requests | Token expirou (60min) | Faça login novamente |
| 403 ao logar | E-mail não confirmado | Use `/api/auth/resend-verification` |
| 415 ao fazer upload | Tipo de arquivo não permitido | Aceitos: pdf, doc, docx, odt, rtf, txt |
| 410 ao confirmar reunião pelo e-mail | Token expirou (12h) | Reagende a reunião |
| 410 ao redefinir senha | Token de recuperação expirou (1h) | Solicite uma nova recuperação |
| 429 ao logar | Excedeu rate limit (5/min/IP) | Aguarde o tempo do `Retry-After` |
| Frontend não acha a API em prod | nginx do container precisa do `backend` no mesmo network | Confira `docker compose ps` |
| `.env` não está sendo lido | Compose 1.x velho | Use Compose v2 (`docker compose`, não `docker-compose`) |

---

## Estrutura do projeto

```
sgtc/
├── api/                          # Backend Spring Boot
│   ├── src/main/java/...
│   ├── src/main/resources/
│   │   ├── application.properties        # base / dev
│   │   ├── application-prod.properties   # overrides prod
│   │   ├── db/migration/                 # Flyway
│   │   └── templates/reuniao-template.pdf
│   ├── Dockerfile
│   └── pom.xml
├── web/                          # Frontend Angular
│   ├── src/app/...
│   ├── nginx.conf                # proxy /api -> backend:8080
│   ├── proxy.conf.json           # dev only
│   └── Dockerfile
├── .env.example
├── docker-compose.yml
└── pom.xml                       # parent pom
```
