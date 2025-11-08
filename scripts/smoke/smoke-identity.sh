#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${IDENTITY_BASE_URL:-}"

resolve_base_url() {
  if [[ -n "$BASE_URL" ]]; then
    echo "${BASE_URL%/}"
    return
  fi
  for c in "http://localhost:8181" "http://localhost:8081"; do
    if curl -fsS --max-time 2 "$c/q/health" >/dev/null 2>&1; then
      echo "$c"
      return
    fi
  done
  echo "http://localhost:8181"
}

test_http() {
  local url="$1"; shift
  local expected=("$@")
  local code
  code=$(curl -sS -o /dev/null -w "%{http_code}" --max-time 5 "$url" || echo 0)
  local ok=1
  for e in "${expected[@]}"; do
    [[ "$code" == "$e" ]] && ok=0 && break
  done
  echo "$code" "$ok"
}

test_tcp() {
  local host="$1" port="$2"
  if (echo > /dev/tcp/$host/$port) >/dev/null 2>&1; then
    echo "1"
  else
    echo "0"
  fi
}

echo "[smoke] Tenancy-Identity + Kafka quick checks"
BASE=$(resolve_base_url)
echo "[smoke] Using base URL: $BASE"

read -r HEALTH_CODE _ < <(test_http "$BASE/q/health" 200)
read -r TEMPLATES_CODE TEMPLATES_ERR < <(test_http "$BASE/api/roles/templates" 200)

KAFKA_TCP=$(test_tcp localhost 9092)
KAFKA_UI_TCP=$(test_tcp localhost 8090)
read -r KAFKA_UI_HTTP _ < <(test_http "http://localhost:8090/" 200 301 302)

echo "[smoke] Identity health: ${HEALTH_CODE}"
echo "[smoke] Role templates: ${TEMPLATES_CODE} (ok=$([[ "$TEMPLATES_ERR" == "0" ]] && echo true || echo false))"
echo "[smoke] Kafka 9092 reachable: $([[ "$KAFKA_TCP" == "1" ]] && echo true || echo false)"
echo "[smoke] Kafka UI 8090 reachable: $([[ "$KAFKA_UI_TCP" == "1" ]] && echo true || echo false)); HTTP=${KAFKA_UI_HTTP}"

FAIL=0
if [[ "$TEMPLATES_ERR" != "0" ]]; then
  FAIL=1
fi

if [[ "$HEALTH_CODE" != "200" ]]; then
  echo "[warn] /q/health not 200 (may be disabled)." >&2
fi

if [[ "$FAIL" != "0" ]]; then
  echo "[smoke] One or more critical checks failed." >&2
  exit 1
fi

echo "[smoke] All critical checks passed."

