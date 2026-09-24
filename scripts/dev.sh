#!/usr/bin/env bash
# Local dev: starts the bundled MySQL (docker compose, profile localdb) and runs the app in TomEE embedded on :8080.
# Reads settings from .env. Set KABU_DEV_AUTH=true there to skip Authentik.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo ".env missing - copy .env.example to .env and fill it in" >&2
  exit 1
fi
set -a
# shellcheck disable=SC1091
. ./.env
set +a

docker compose --profile localdb up -d mysql
echo "waiting for MySQL ..."
until docker compose exec -T mysql mysqladmin ping -h localhost --silent >/dev/null 2>&1; do sleep 2; done

exec mvn -B clean package -DskipTests tomee-embedded:run \
  -Dkabu.dev.db.url="jdbc:mysql://localhost:3306/${KABU_DB_DATABASE:-kabuproxy}" \
  -Dkabu.dev.db.username="${KABU_DB_USERNAME:-kabuproxy}" \
  -Dkabu.dev.db.password="${KABU_DB_PASSWORD:-kabuproxy}"
