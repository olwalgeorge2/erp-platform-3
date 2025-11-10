#!/usr/bin/env bash
set -euo pipefail

ACTION="${1:-up}"
ROOT_DIR="$(cd "$(dirname "$0")"/.. && pwd)"
COMPOSE_FILE="$ROOT_DIR/monitoring/docker-compose.yml"

if ! command -v docker >/dev/null 2>&1; then
  echo "docker is required"
  exit 1
fi

case "$ACTION" in
  up)
    docker compose -f "$COMPOSE_FILE" up -d
    echo "Monitoring stack started (Prometheus:9090, Grafana:3000)"
    ;;
  down)
    docker compose -f "$COMPOSE_FILE" down
    echo "Monitoring stack stopped"
    ;;
  restart)
    docker compose -f "$COMPOSE_FILE" down
    docker compose -f "$COMPOSE_FILE" up -d
    echo "Monitoring stack restarted"
    ;;
  logs)
    docker compose -f "$COMPOSE_FILE" logs -f
    ;;
  *)
    echo "Usage: $0 [up|down|restart|logs]"
    exit 1
    ;;
esac

