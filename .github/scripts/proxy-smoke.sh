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

# Wait for Python HTTP server to be ready
echo "Waiting for Python HTTP server on :18081..."
for i in {1..30}; do
  if curl -fsS "http://localhost:18081/" >/dev/null 2>&1; then
    echo "Python HTTP server ready"
    break
  fi
  if [ $i -eq 30 ]; then
    echo "Python HTTP server failed to start. Logs:" >&2
    cat "$PY_LOG" >&2
    exit 1
  fi
  sleep 1
done

# Test that mock directory is accessible
if ! curl -fsS "http://localhost:18081/mock/" >/dev/null 2>&1; then
  echo "Warning: Mock directory not accessible at http://localhost:18081/mock/" >&2
  echo "Available paths:" >&2
  curl -s "http://localhost:18081/" | head -20 >&2 || true
fi

# Run gateway with a simple route to the mock backend
PORT=8080
export QUARKUS_HTTP_PORT=$PORT
export JWT_ENABLED=false
JAR="api-gateway/build/quarkus-app/quarkus-run.jar"
CONFIG_FILE=".github/scripts/proxy-smoke-application.yml"

java -Dquarkus.config.locations="$CONFIG_FILE" -jar "$JAR" >/tmp/gateway-proxy-smoke.log 2>&1 &
GW_PID=$!

# Wait for live with early error detection
for i in {1..60}; do
  if curl -fsS "http://localhost:$PORT/q/health/live" >/dev/null 2>&1; then
    break
  fi
  # Check if process died early
  if ! kill -0 "$GW_PID" 2>/dev/null; then
    echo "Gateway process died during startup. First 50 lines of error:" >&2
    head -n 50 /tmp/gateway-proxy-smoke.log >&2
    exit 1
  fi
  sleep 1
done

# Final check if we never connected
if ! curl -fsS "http://localhost:$PORT/q/health/live" >/dev/null 2>&1; then
  echo "Gateway did not become live in time. Logs:" >&2
  tail -n 100 /tmp/gateway-proxy-smoke.log >&2
  exit 1
fi

# Probe proxy (follow redirects; fall back to explicit index)
STATUS=$(curl -fsSL -o /dev/null -w "%{http_code}\n" "http://localhost:$PORT/mock/" || true)
if [[ "$STATUS" != "200" ]]; then
  STATUS=$(curl -fsSL -o /dev/null -w "%{http_code}\n" "http://localhost:$PORT/mock/index.html" || true)
fi

if [[ "$STATUS" != "200" ]]; then
  echo "Proxy smoke failed; expected 200, got $STATUS. Logs:" >&2
  tail -n 200 /tmp/gateway-proxy-smoke.log || true
  exit 1
fi
echo "Proxy smoke OK"
