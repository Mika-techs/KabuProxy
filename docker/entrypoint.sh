#!/bin/sh
# Writes the KABU_DB_* env vars as TomEE resource overrides (<resourceId>.<property>) into
# conf/system.properties, so no secret ever needs to be sed-escaped into tomee.xml.
set -e

: "${KABU_DB_HOST:?KABU_DB_HOST is required}"
: "${KABU_DB_DATABASE:?KABU_DB_DATABASE is required}"
: "${KABU_DB_USERNAME:?KABU_DB_USERNAME is required}"
: "${KABU_DB_PASSWORD:?KABU_DB_PASSWORD is required}"
KABU_DB_PORT=${KABU_DB_PORT:-3306}
KABU_DB_JDBC_PARAMS=${KABU_DB_JDBC_PARAMS#\?}

JDBC_URL="jdbc:mysql://$KABU_DB_HOST:$KABU_DB_PORT/$KABU_DB_DATABASE"
if [ -n "$KABU_DB_JDBC_PARAMS" ]; then
  JDBC_URL="$JDBC_URL?$KABU_DB_JDBC_PARAMS"
fi

# java.util.Properties: escape backslashes, keep everything else literal
escape() { printf '%s' "$1" | sed 's/\\/\\\\/g'; }

PROPS=/usr/local/tomee/conf/system.properties
sed -i '/^KabuDataSource\./d' "$PROPS"
{
  printf 'KabuDataSource.JdbcUrl=%s\n' "$(escape "$JDBC_URL")"
  printf 'KabuDataSource.UserName=%s\n' "$(escape "$KABU_DB_USERNAME")"
  printf 'KabuDataSource.Password=%s\n' "$(escape "$KABU_DB_PASSWORD")"
} >> "$PROPS"

if [ "$KABU_DEV_AUTH" = "true" ]; then
  echo "WARNING: KABU_DEV_AUTH=true - authentication is DISABLED, every visitor is admin!" >&2
fi

exec "$@"
