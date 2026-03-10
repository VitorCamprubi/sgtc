# SGTC - Sistema de Gestao de TCC

Projeto exemplo com:
- API em Java (Spring Boot) na pasta `api`
- Frontend em Angular 20 na pasta `web`
- Stack Docker na raiz do repositorio

## Recursos principais
- Autenticacao com JWT Bearer (`/api/auth/login`)
- Controle de acesso por perfil (`ADMIN`, `ORIENTADOR`, `COORIENTADOR`, `ALUNO`)
- Gestao de grupos e membros
- Upload, listagem, download e exclusao de documentos
- Comentarios em documentos
- Reunioes por grupo

## Requisitos para execucao local (sem Docker)
- Java 21+
- Maven 3.9+ (ou wrapper `mvnw` / `mvnw.cmd`)
- Node.js 18+ e NPM 9+
- MySQL 8+ rodando em `localhost:3307` (padrao do projeto)

## Variaveis de configuracao da API
Arquivo: `api/src/main/resources/application.properties`

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3307/sgtc?createDatabaseIfNotExist=true&serverTimezone=UTC}
spring.datasource.username=${DB_USER:sgtc}
spring.datasource.password=${DB_PASS:sgtc2420}
security.jwt.secret=${JWT_SECRET:sgtc-dev-jwt-secret-key-32-bytes-minimum}
```

## Como executar localmente
### 1) Backend
Windows:
```bat
cd api
mvnw.cmd spring-boot:run
```

Linux/macOS:
```bash
cd api
./mvnw spring-boot:run
```

API em `http://localhost:8080`.

### 2) Frontend
```bash
cd web
npm install
npm start
```

Frontend em `http://localhost:4200`.

No modo dev, o Angular faz proxy de `/api/**` e `/public/**` para `http://localhost:8080` via `web/proxy.conf.json`.

## Credenciais seed (DataLoader)
- ADMIN: `admin@sgtc.local` / `admin123`
- ORIENTADOR: `orientador@sgtc.local` / `ori123`
- ALUNO: `aluno@sgtc.local` / `aluno123`

## Docker (stack completa)
O `docker-compose.yml` na raiz sobe:
- MySQL
- Backend
- Frontend (Nginx com proxy para backend em `/api` e `/public`)
- Adminer

### Passos
1. Copie o arquivo de exemplo:
```bash
cp .env.example .env
```

No Windows, copie manualmente `.env.example` para `.env`.

2. Suba a stack:
```bash
docker compose up -d --build
```

3. URLs padrao:
- Frontend: `http://localhost:4200`
- Backend API: `http://localhost:8080`
- Adminer: `http://localhost:8081`
- MySQL: `localhost:${MYSQL_PORT}` (padrao `3307`)

4. Para parar:
```bash
docker compose down
```

## Endpoints principais
### Auth
- `POST /api/auth/login`
- `GET /api/auth/me`

### Grupos
- `POST /api/grupos` (ADMIN)
- `GET /api/grupos/me`
- `GET /api/grupos/{id}/membros`
- `POST /api/grupos/{id}/membros` (ADMIN)

### Documentos
- `POST /api/grupos/{grupoId}/documentos`
- `GET /api/grupos/{grupoId}/documentos`
- `GET /api/documentos/{docId}/download`
- `DELETE /api/documentos/{docId}`

### Comentarios
- `GET /api/documentos/{docId}/comentarios`
- `POST /api/documentos/{docId}/comentarios`

### Reunioes
- `GET /api/grupos/{grupoId}/reunioes`
- `POST /api/grupos/{grupoId}/reunioes`

### Publico
- `GET /public/health`
- `GET /public/debug/users`

## Troubleshooting rapido
- `401/403`: confirme login JWT e perfil do usuario.
- Frontend Docker sem API: confirme que o container `frontend` esta com proxy Nginx (arquivo `web/nginx.conf`).
- Erro de segredo JWT: use chave com pelo menos 32 bytes em `JWT_SECRET`.
- Porta MySQL ocupada: ajuste `MYSQL_PORT` no `.env`.
