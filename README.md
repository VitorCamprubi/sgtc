# SGTC — Sistema de Gestão de TCC

SGTC é um projeto exemplo composto por:
- API em Java (Spring Boot) na pasta `api`
- Frontend em Angular 20 na pasta `web`

Ele ilustra um fluxo típico de TCC com grupos, upload de documentos (versões), comentários, reuniões e controle de acesso por papéis (ADMIN, ORIENTADOR, COORIENTADOR, ALUNO).

## Recursos implementados
- Autenticação via HTTP Basic (sem endpoint de login; o front envia o header Authorization)
- Grupos
  - Listar “meus grupos” (de acordo com o papel do usuário)
  - Criar grupos (ADMIN)
  - Adicionar membros (ADMIN)
- Documentos
  - Upload (multipart), versões por grupo
  - Listagem e download por versão
  - Exclusão (ADMIN, ORIENTADOR/COORIENTADOR, ou o autor do envio)
- Comentários de documentos
  - Listar (quem pode acessar o grupo)
  - Incluir (ORIENTADOR/COORIENTADOR/ADMIN)
- Reuniões
  - Listar por grupo (todos do grupo)
  - Agendar (ORIENTADOR/COORIENTADOR/ADMIN)

## Requisitos
- Java 17+ (JDK)
- Maven 3.9+ (ou o wrapper incluído `mvnw`/`mvnw.cmd`)
- Node.js 18+ e NPM 9+
- MySQL 8+ em `localhost:3306`

> Dica: ajuste as credenciais do banco em `api/src/main/resources/application.properties` se necessário.

## Como baixar e executar

### 1) Clonar o repositório
```bash
# via HTTPS (exemplo)
git clone <URL_DO_REPOSITORIO>
cd sgtc
```

### 2) Configurar o banco de dados
O backend usa MySQL com as configurações padrão abaixo (arquivo `application.properties`):
```
spring.datasource.url=jdbc:mysql://localhost:3306/sgtc?createDatabaseIfNotExist=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=212420
```
- Ajuste `username`/`password` conforme seu ambiente
- O schema `sgtc` será criado automaticamente (DDL auto: `update`)

O backend cria usuários de exemplo (DataLoader) na primeira execução:
- ADMIN: `admin@sgtc.local` / `admin123`
- ORIENTADOR: `orientador@sgtc.local` / `ori123`
- ALUNO: `aluno@sgtc.local` / `aluno123`

### 3) Subir o backend (API)
No Windows:
```bat
cd api
mvnw.cmd spring-boot:run
```
No Linux/macOS:
```bash
cd api
./mvnw spring-boot:run
```
A API ficará em `http://localhost:8080`.

CORS: por padrão permite `http://localhost:4200` (Angular). Ajuste a chave `app.cors.allowed-origins` se mudar a porta/origem do front.

### 4) Subir o frontend (Angular)
Em outro terminal:
```bash
cd web
npm install
npm start
```
- O app abrirá em `http://localhost:4200`
- Proxy de desenvolvimento envia `/api/**` e `/public/**` para `http://localhost:8080` (ver `web/proxy.conf.json`)

### 5) Login e navegação
- Faça login com quaisquer usuários de exemplo acima
- Página “Grupos”: lista seus grupos; se ADMIN, aparece o formulário “Criar novo grupo”
- Clique no título de um grupo para abrir os detalhes (documentos, comentários, reuniões e ações de admin)

## Referência rápida de endpoints (API)
- Saúde/Ping
  - `GET /public/health` (público)
  - `GET /api/ping` (protegido — requer Basic Auth)
- Grupos
  - `POST /api/grupos` (ADMIN) — cria grupo
  - `GET /api/grupos/me` — lista grupos do usuário
  - `POST /api/grupos/{id}/membros` (ADMIN) — adiciona membros (alunos)
- Documentos
  - `POST /api/grupos/{grupoId}/documentos` — upload (multipart: `titulo`, `file`)
  - `GET  /api/grupos/{grupoId}/documentos` — listar versões
  - `GET  /api/documentos/{docId}/download` — download
  - `DELETE /api/documentos/{docId}` — excluir (ADMIN / ORIENTADOR / COORIENTADOR / autor)
- Comentários
  - `GET  /api/documentos/{docId}/comentarios` — listar
  - `POST /api/documentos/{docId}/comentarios` — comentar (ORIENTADOR/COORIENTADOR/ADMIN)
- Reuniões
  - `GET  /api/grupos/{grupoId}/reunioes` — listar
  - `POST /api/grupos/{grupoId}/reunioes` — agendar
- Debug (somente desenvolvimento)
  - `GET /public/debug/users` — lista usuários seed

## Variáveis/Configs úteis
- `app.cors.allowed-origins` — origem do front (CORS)
- `app.upload.dir` — diretório base para uploads (padrão `uploads` dentro de `api/`)
- `spring.servlet.multipart.max-file-size`/`max-request-size` — limites de upload (padrão 20MB)

## Solução de problemas
- 401/403 nas chamadas à API
  - Verifique se o login está ativo; o front guarda `Authorization` em `sessionStorage/localStorage` como `sgtc_auth`
  - Confirme o papel do usuário para ações restritas (comentários, reuniões, admin)
- CORS
  - Execute o front em `http://localhost:4200` ou atualize `app.cors.allowed-origins`
- Upload não salva
  - Título e arquivo são obrigatórios
  - Tamanho do arquivo ≤ 20MB
  - Verifique permissões do usuário para o grupo
- Navegação para detalhes do grupo
  - Clique no título da linha na tabela de Grupos
  - Ou acesse diretamente `http://localhost:4200/grupos/<id>`

## Licença
Uso educacional/demonstração. Adapte conforme sua necessidade.

