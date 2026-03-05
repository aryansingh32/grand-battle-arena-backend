#!/bin/sh
set -e

# Convert Render/Railway DATABASE_URL (postgres:// or postgresql://) to JDBC if needed.
if [ -z "${JDBC_DATABASE_URL:-}" ] && [ -n "${DATABASE_URL:-}" ]; then
  case "$DATABASE_URL" in
    postgres://*|postgresql://*)
      JDBC_DATABASE_URL="jdbc:${DATABASE_URL}"
      export JDBC_DATABASE_URL
      echo "Converted DATABASE_URL -> JDBC_DATABASE_URL"
      ;;
  esac
fi

# Prefer managed Redis URL if provided by platform.
if [ -z "${SPRING_DATA_REDIS_URL:-}" ] && [ -n "${REDIS_URL:-}" ]; then
  export SPRING_DATA_REDIS_URL="$REDIS_URL"
fi

exec java $JAVA_OPTS -jar app.jar

