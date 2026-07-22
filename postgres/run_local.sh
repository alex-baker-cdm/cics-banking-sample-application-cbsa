#!/usr/bin/env bash
# Build & test the normalized CBSA Postgres schema locally in a throwaway
# Docker container. Requires Docker.
set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONTAINER=cbsa-pg-test
PGPASSWORD=cbsa
DBNAME=cbsa

cleanup() { docker rm -f "$CONTAINER" >/dev/null 2>&1 || true; }
trap cleanup EXIT
cleanup

echo ">> starting postgres:16 ..."
docker run -d --name "$CONTAINER" \
  -e POSTGRES_PASSWORD="$PGPASSWORD" -e POSTGRES_DB="$DBNAME" \
  postgres:16 >/dev/null

echo ">> waiting for postgres to be ready ..."
for i in $(seq 1 30); do
  if docker exec "$CONTAINER" pg_isready -U postgres >/dev/null 2>&1; then break; fi
  sleep 1
done

run() { docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -U postgres -d "$DBNAME" -q < "$1"; }

echo ">> applying schema ..."
run "$HERE/schema/01_schema.sql"
run "$HERE/schema/02_sequences_and_views.sql"
echo ">> seeding lookups + sample data ..."
run "$HERE/seed/01_lookups.sql"
run "$HERE/seed/02_sample_data.sql"
echo ">> running tests ..."
docker exec -i "$CONTAINER" psql -v ON_ERROR_STOP=1 -U postgres -d "$DBNAME" < "$HERE/test/01_tests.sql"

echo ">> DONE"
