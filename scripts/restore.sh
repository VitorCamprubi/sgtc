#!/usr/bin/env bash
# =============================================================
# SGTC - restore de backup
# =============================================================
# Restaura um par de arquivos db-<timestamp>.sql.gz e
# uploads-<timestamp>.tar.gz gerados por backup.sh.
#
# Uso:
#   ./scripts/restore.sh 20260506-020000
#
# CUIDADO: este script DROPA e recria o schema. Tem flag
# --confirm-yes para automacoes; por padrao pede confirmacao.
# =============================================================

set -euo pipefail

if [ "${1:-}" = "" ] || [ "${1:-}" = "--help" ] || [ "${1:-}" = "-h" ]; then
    echo "Uso: $0 <timestamp> [--confirm-yes]"
    echo "Ex.: $0 20260506-020000"
    exit 1
fi

TIMESTAMP="$1"
shift || true
AUTO_YES="${1:-}"

PROJECT_DIR="${PROJECT_DIR:-$(cd "$(dirname "$0")/.." && pwd)}"
BACKUP_DIR="${BACKUP_DIR:-${PROJECT_DIR}/backups}"
COMPOSE_FILE="${COMPOSE_FILE:-${PROJECT_DIR}/docker-compose.yml}"
ENV_FILE="${ENV_FILE:-${PROJECT_DIR}/.env}"

MYSQL_SVC="${MYSQL_SVC:-mysql}"
UPLOADS_VOLUME="${UPLOADS_VOLUME:-sgtc_sgtc_uploads}"

DUMP_FILE="${BACKUP_DIR}/db-${TIMESTAMP}.sql.gz"
UPLOADS_FILE="${BACKUP_DIR}/uploads-${TIMESTAMP}.tar.gz"

[ -f "$DUMP_FILE" ]    || { echo "Nao encontrado: $DUMP_FILE"; exit 1; }
[ -f "$UPLOADS_FILE" ] || { echo "Nao encontrado: $UPLOADS_FILE"; exit 1; }
[ -f "$ENV_FILE" ]     || { echo "Nao encontrado: $ENV_FILE"; exit 1; }

# shellcheck disable=SC2046
export $(grep -v '^#' "$ENV_FILE" | grep -E '^[A-Z_]+=' | xargs -d '\n')

: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD nao definido no .env}"
: "${MYSQL_DATABASE:?MYSQL_DATABASE nao definido no .env}"

if [ "$AUTO_YES" != "--confirm-yes" ]; then
    echo "ATENCAO: o banco '$MYSQL_DATABASE' sera DROPADO e recriado."
    echo "         O volume $UPLOADS_VOLUME tera o conteudo SUBSTITUIDO."
    read -r -p "Continuar? (digite 'sim' para confirmar): " resposta
    [ "$resposta" = "sim" ] || { echo "Abortado."; exit 1; }
fi

echo "Recriando schema..."
docker compose -f "$COMPOSE_FILE" exec -T "$MYSQL_SVC" \
    mysql -u root -p"$MYSQL_ROOT_PASSWORD" \
    -e "DROP DATABASE IF EXISTS ${MYSQL_DATABASE}; CREATE DATABASE ${MYSQL_DATABASE} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

echo "Restaurando dump..."
gunzip -c "$DUMP_FILE" | docker compose -f "$COMPOSE_FILE" exec -T "$MYSQL_SVC" \
    mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"

echo "Restaurando uploads..."
docker run --rm \
    -v "${UPLOADS_VOLUME}":/data \
    -v "${BACKUP_DIR}":/backup:ro \
    alpine \
    sh -c "rm -rf /data/* && tar xzf /backup/uploads-${TIMESTAMP}.tar.gz -C /data"

echo "Restore concluido. Reinicie o backend:"
echo "  docker compose -f $COMPOSE_FILE restart backend"
