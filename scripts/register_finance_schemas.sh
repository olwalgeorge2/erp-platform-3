#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCHEMA_DIR="$ROOT_DIR/docs/schemas/finance"
REGISTRY_URL="${SCHEMA_REGISTRY_URL:-http://localhost:8085}"
AUTH_HEADER=()

if [[ -n "${SCHEMA_REGISTRY_BASIC_AUTH:-}" ]]; then
  AUTH_HEADER=(-H "Authorization: Basic ${SCHEMA_REGISTRY_BASIC_AUTH}")
fi

register_schema() {
  local subject="$1"
  local file="$2"
  echo "Registering ${subject} from ${file} to ${REGISTRY_URL}"
  curl -sS -X POST \
    -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    "${AUTH_HEADER[@]}" \
    "${REGISTRY_URL}/subjects/${subject}/versions" \
    -d @"${file}" | jq .
}

register_schema "finance.journal.events.v1-value"       "${SCHEMA_DIR}/finance.journal.events.v1.json"
register_schema "finance.period.events.v1-value"        "${SCHEMA_DIR}/finance.period.events.v1.json"
register_schema "finance.reconciliation.events.v1-value" "${SCHEMA_DIR}/finance.reconciliation.events.v1.json"

echo "Done."
