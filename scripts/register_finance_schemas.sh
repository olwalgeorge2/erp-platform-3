#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SCHEMA_DIR="$ROOT_DIR/docs/schemas/finance"
REGISTRY_URL="${SCHEMA_REGISTRY_URL:-http://localhost:18081}"
AUTH_HEADER=()

if [[ -n "${SCHEMA_REGISTRY_BASIC_AUTH:-}" ]]; then
  AUTH_HEADER=(-H "Authorization: Basic ${SCHEMA_REGISTRY_BASIC_AUTH}")
fi

register_schema() {
  local subject="$1"
  local file="$2"
  local schema_type="${3:-JSON}"
  
  echo "Registering ${subject} from ${file} to ${REGISTRY_URL}"
  
  # Read the raw schema and escape it for JSON
  local raw_schema=$(cat "${file}" | jq -c .)
  local payload=$(jq -n \
    --arg schema "${raw_schema}" \
    --arg schemaType "${schema_type}" \
    '{schemaType: $schemaType, schema: $schema}')
  
  local response=$(curl -sS -w "\n%{http_code}" -X POST \
    -H "Content-Type: application/vnd.schemaregistry.v1+json" \
    "${AUTH_HEADER[@]}" \
    "${REGISTRY_URL}/subjects/${subject}/versions" \
    -d "${payload}")
  
  local http_code=$(echo "${response}" | tail -n1)
  local body=$(echo "${response}" | sed '$d')
  
  if [[ "${http_code}" =~ ^2 ]]; then
    echo "✓ Registered successfully: ${body}" | jq .
  else
    echo "✗ Registration failed (HTTP ${http_code}): ${body}"
    return 1
  fi
}

register_schema "finance.journal.events.v1-value"       "${SCHEMA_DIR}/finance.journal.events.v1.json"
register_schema "finance.period.events.v1-value"        "${SCHEMA_DIR}/finance.period.events.v1.json"
register_schema "finance.reconciliation.events.v1-value" "${SCHEMA_DIR}/finance.reconciliation.events.v1.json"

echo "Done."
