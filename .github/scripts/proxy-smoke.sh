#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")"/../.. && pwd)"
cd "$ROOT_DIR"

# Build gateway fast-jar
./gradlew :api-gateway:quarkusBuild -x test -Dquarkus.package.type=fast-jar

# Start a simple backend on 18081
PY_LOG="/tmp/mock-backend.log"
if command -v python3 >/dev/null 2>&1; then
  python3 -m http.server 18081 >"$PY_LOG" 2>&1 &
  PY_PID=$!
else
  echo "python3 not available; cannot run proxy smoke" >&2; exit 1
fi

cleanup() {
  [ -n "${GW_PID:-}" ] && kill "$GW_PID" >/dev/null 2>&1 || true
  [ -n "${PY_PID:-}" ] && kill "$PY_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

# Run gateway with a single route to the mock backend
PORT=8080
export QUARKUS_HTTP_PORT=$PORT
export JWT_ENABLED=false
JAR="api-gateway/build/quarkus-app/quarkus-run.jar"

JAVA_OPTS=(
  -Dgateway.routes[0].pattern=/mock/*
  -Dgateway.routes[0].base-url=http://localhost:18081
  -Dgateway.routes[0].timeout=PT3S
  -Dgateway.routes[0].retries=0
  -Dgateway.routes[0].auth-required=false
  -Dgateway.routes[0].health-path=/
)

java "${JAVA_OPTS[@]}" -jar "$JAR" >/tmp/gateway-proxy-smoke.log 2>&1 &
GW_PID=$!

# Wait for live
for i in {1..60}; do
  if curl -fsS "http://localhost:$PORT/q/health/live" >/dev/null; then
    break
  fi
  sleep 1
done

# Probe proxy
curl -fsS -o /dev/null -w "%{http_code}\n" "http://localhost:$PORT/mock/" | grep -q '^200$' || {
  echo "Proxy smoke failed; logs:" >&2
  tail -n 200 /tmp/gateway-proxy-smoke.log || true
  exit 1
}
echo "Proxy smoke OK"

