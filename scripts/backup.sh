#!/usr/bin/env bash
# =============================================================
# SGTC - backup diario
# =============================================================
# Faz dump do banco MySQL e tar dos uploads, com retencao
# configuravel. Pensado para rodar em cron no host de producao.
#
# Uso:
#   ./scripts/backup.sh                # diretorio padrao ./backups
#   BACKUP_DIR=/var/backups/sgtc ./scripts/backup.sh
#
# Cron (exemplo, todo dia as 02:00):
#   0 2 * * * cd /opt/sgtc && ./scripts/backup.sh >> /var/log/sgtc-backup.log 2>&1
#
# Variaveis de ambiente esperadas (lidas do .env do projeto):
#   MYSQL_ROOT_PASSWORD, MYSQL_DATABASE
# =============================================================

set -euo pipefail

# --- Configuracao ---------------------------------------------------
PROJECT_DIR="${PROJECT_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
BACKUP_DIR="${BACKUP_DIR:-${PROJECT_DIR}/backups}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
COMPOSE_FILE="${COMPOSE_FILE:-${PROJECT_DIR}/docker-compose.yml}"
ENV_FILE="${ENV_FILE:-${PROJECT_DIR}/.env}"

# Container names (defaults do compose).
MYSQL_SVC="${MYSQL_SVC:-mysql}"
UPLOADS_VOLUME="${UPLOADS_VOLUME:-sgtc_sgtc_uploads}"

# --- Helpers --------------------------------------------------------
log() {
    printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

err() {
    log "ERRO: $*" >&2
    exit 1
}

# --- Carrega variaveis do .env --------------------------------------
[ -f "$ENV_FILE" ] || err ".env nao encontrado em $ENV_FILE"
# shellcheck disable=SC2046
export $(grep -v '^#' "$ENV_FILE" | grep -E '^[A-Z_]+=' | xargs -d '\n')

: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD nao definido no .env}"
: "${MYSQL_DATABASE:?MYSQL_DATABASE nao definido no .env}"

mkdir -p "$BACKUP_DIR"

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
DUMP_FILE="${BACKUP_DIR}/db-${TIMESTAMP}.sql.gz"
UPLOADS_FILE="${BACKUP_DIR}/uploads-${TIMESTAMP}.tar.gz"

# --- 1) Dump do banco ----------------------------------------------
log "Iniciando dump do MySQL..."
docker compose -f "$COMPOSE_FILE" exec -T "$MYSQL_SVC" \
    mysqldump --single-transaction --quick --routines --triggers \
              -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" \
    | gzip > "$DUMP_FILE"

[ -s "$DUMP_FILE" ] || err "dump do banco esta vazio"
log "Dump OK: $DUMP_FILE ($(du -h "$DUMP_FILE" | cut -f1))"

# --- 2) Backup dos uploads ------------------------------------------
log "Iniciando backup dos uploads..."
docker run --rm \
    -v "${UPLOADS_VOLUME}":/data:ro \
    -v "${BACKUP_DIR}":/backup \
    alpine \
    tar czf "/backup/uploads-${TIMESTAMP}.tar.gz" -C /data .

[ -f "$UPLOADS_FILE" ] || err "tar dos uploads nao foi criado"
log "Uploads OK: $UPLOADS_FILE ($(du -h "$UPLOADS_FILE" | cut -f1))"

# --- 3) Retencao ----------------------------------------------------
log "Removendo backups com mais de $RETENTION_DAYS dias..."
find "$BACKUP_DIR" -maxdepth 1 -type f \
    \( -name 'db-*.sql.gz' -o -name 'uploads-*.tar.gz' \) \
    -mtime +"$RETENTION_DAYS" -print -delete || true

log "Backup concluido com sucesso."
