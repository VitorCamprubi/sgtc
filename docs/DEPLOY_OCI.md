# Deploy do SGTC na OCI

Guia direto para subir o SGTC em uma VM Oracle Cloud Infrastructure usando Docker Compose, MySQL local em volume Docker e Caddy com HTTPS automatico.

## 1. Criar a instancia

Na tela de criacao da instancia:

- Imagem: `Ubuntu 24.04` ou `Ubuntu 22.04`.
- Shape Free Tier recomendado: `VM.Standard.A1.Flex`.
- OCPUs/memoria: `1 OCPU / 6 GB RAM` para comecar.
- Rede: crie ou selecione uma VCN com **sub-rede publica**.
- IPv4 publico: marque **Designar endereco IPv4 publico automaticamente**.
- IPv6: pode deixar desligado.
- SSH: adicione sua chave publica.

Depois de criar, anote o IP publico da instancia.

## 2. Liberar portas na OCI

Na subnet publica, adicione regras de entrada no Security List ou Network Security Group:

| Porta | Protocolo | Origem | Uso |
| --- | --- | --- | --- |
| 22 | TCP | Seu IP, se possivel | SSH |
| 80 | TCP | `0.0.0.0/0` | HTTP / desafio Let's Encrypt |
| 443 | TCP | `0.0.0.0/0` | HTTPS |

Nao libere `3307`, `8080` ou `4200` para a internet em producao.

## 3. Preparar a VM

Conecte via SSH:

```bash
ssh ubuntu@IP_PUBLICO_DA_VM
```

Instale Docker e Git:

```bash
sudo apt update
sudo apt install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo tee /etc/apt/keyrings/docker.asc >/dev/null
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list >/dev/null
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"
newgrp docker
```

Ative tambem o firewall do Ubuntu:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable
```

## 4. Enviar o projeto

Opcao com Git:

```bash
sudo mkdir -p /opt/sgtc
sudo chown "$USER:$USER" /opt/sgtc
git clone URL_DO_REPOSITORIO /opt/sgtc
cd /opt/sgtc
```

Opcao sem Git, a partir do seu computador:

```bash
scp -r C:/Users/Vitor/Desktop/Projetos-IntelliJ/sgtc ubuntu@IP_PUBLICO_DA_VM:/opt/sgtc
```

## 5. Configurar o .env de producao

Na VM:

```bash
cd /opt/sgtc
cp .env.example .env
nano .env
```

Valores obrigatorios:

```env
SPRING_PROFILES_ACTIVE=prod
MYSQL_DATABASE=sgtc
MYSQL_USER=sgtc
MYSQL_PASSWORD=SENHA_FORTE_DO_MYSQL
MYSQL_ROOT_PASSWORD=SENHA_FORTE_ROOT
JWT_SECRET=SECRET_GERADO_COM_OPENSSL
APP_CORS_ALLOWED_ORIGINS=https://SEU_DOMINIO
MAIL_ENABLED=true
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=SEU_EMAIL
MAIL_PASSWORD=SUA_APP_PASSWORD
MAIL_FROM=SEU_EMAIL
MAIL_FROM_NAME=SGTC
APP_API_URL=https://SEU_DOMINIO
APP_WEB_URL=https://SEU_DOMINIO
SGTC_DOMAIN=SEU_DOMINIO
JAVA_OPTS=-Xms256m -Xmx768m
```

Gere o JWT secret na VM:

```bash
openssl rand -base64 64
```

Se ainda nao tiver dominio apontando para o IP da VM, use `SGTC_DOMAIN=:80` temporariamente e acesse por `http://IP_PUBLICO_DA_VM`. Para HTTPS automatico, crie um registro DNS `A` apontando o dominio para o IP publico e use `SGTC_DOMAIN=seudominio.com`.

## 6. Subir a aplicacao

Com dominio e portas 80/443 liberadas:

```bash
cd /opt/sgtc
docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile https up -d --build
```

Verifique:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile https ps
docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile https logs -f backend
docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile https logs -f caddy
```

Teste:

```bash
curl -I https://SEU_DOMINIO
curl https://SEU_DOMINIO/api/ping
```

## 7. Criar o primeiro admin

Em producao o seed de usuarios nao roda. Crie o primeiro admin manualmente no banco.

Gere um hash BCrypt da senha:

```bash
docker run --rm httpd:2.4-alpine htpasswd -bnBC 10 "" "SENHA_DO_ADMIN" | tr -d ':\n'
```

Depois insira no MySQL:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --profile https exec mysql mysql -u root -p"$MYSQL_ROOT_PASSWORD" sgtc
```

No prompt do MySQL:

```sql
INSERT INTO users (nome, email, senha_hash, role, email_confirmado)
VALUES ('Admin', 'admin@seudominio.com', 'HASH_BCRYPT_AQUI', 'ADMIN', TRUE);
```

## 8. Backup

Configure backup diario:

```bash
sudo tee /etc/cron.d/sgtc-backup >/dev/null <<'CRON'
0 2 * * * ubuntu cd /opt/sgtc && ./scripts/backup.sh >> /var/log/sgtc-backup.log 2>&1
CRON
```

Copie os backups para fora da VM periodicamente. Volume local da OCI nao substitui backup externo.

## Troubleshooting rapido

- Caddy nao gera HTTPS: confira DNS apontando para a VM e portas `80/443` liberadas na OCI e no `ufw`.
- Backend nao sobe com JWT: gere novo `JWT_SECRET` com `openssl rand -base64 64`.
- Login retorna 403: usuario esta com `email_confirmado=false`.
- Front nao chama API: confira `APP_CORS_ALLOWED_ORIGINS`, `APP_API_URL`, `APP_WEB_URL` e `SGTC_DOMAIN`.
