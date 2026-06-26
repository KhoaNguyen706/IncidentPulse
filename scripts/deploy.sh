#!/usr/bin/env bash
# Deploy / update IncidentPulse on a single host (e.g. EC2 t2.micro).
# Pulls the latest code, rebuilds, and restarts the prod stack.
# Flyway migrations run automatically on app boot.
#
# Usage:  ./scripts/deploy.sh
set -euo pipefail

COMPOSE_FILE="docker-compose.prod.yml"

cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  echo "ERROR: .env not found. Run: cp .env.example .env  (then fill in values)" >&2
  exit 1
fi

echo "==> Pulling latest code"
git pull --ff-only

echo "==> Building and starting containers"
docker compose -f "$COMPOSE_FILE" up -d --build

echo "==> Pruning dangling images"
docker image prune -f >/dev/null 2>&1 || true

echo "==> Status"
docker compose -f "$COMPOSE_FILE" ps

echo "==> Done. Tail logs with:  docker compose -f $COMPOSE_FILE logs -f app"
