#!/usr/bin/env bash
set -euo pipefail

cd "$APP_DIR"

PREVIOUS=$(git rev-parse HEAD)
echo "Current version: $PREVIOUS"

git fetch --prune origin main
git reset --hard origin/main      # does not touch .env, because it is untracked and git-ignored
echo "Deploying version: $(git rev-parse HEAD)"

wait_healthy() {
  for i in $(seq 1 30); do
    if curl -fsS http://localhost:8080/actuator/health/liveness > /dev/null; then
      return 0
    fi
    sleep 5
  done
  return 1
}

if docker compose up -d --build && wait_healthy; then
  echo "Deployment healthy"
  docker image prune -f > /dev/null
  exit 0
fi

echo "Deployment FAILED. Recent app logs:"
docker compose logs app --tail 50 || true

echo "Rolling back to $PREVIOUS"
git reset --hard "$PREVIOUS"
docker compose up -d --build
exit 1