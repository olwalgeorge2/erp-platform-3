#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")"/../.. && pwd)"
cd "$ROOT_DIR"

./gradlew :api-gateway:quarkusBuild -x test -Dquarkus.package.type=fast-jar

PORT=8080
export QUARKUS_HTTP_PORT=$PORT
export JWT_ENABLED=false

JAR="api-gateway/build/quarkus-app/quarkus-run.jar"
if [ ! -f "$JAR" ]; then
  echo "Runner jar not found at $JAR" >&2
  exit 1
fi

echo "Starting API Gateway (fast-jar) on :$PORT"
java -Dquarkus.config.locations=".github/scripts/smoke-application.yml" -jar "$JAR" >/tmp/gateway.log 2>&1 &
PID=$!
echo "PID=$PID"

# Wait for liveness with early error detection
tries=0
until curl -fsS "http://localhost:$PORT/q/health/live" >/dev/null 2>&1; do
  tries=$((tries+1))
  
  # Check if process died early
  if ! kill -0 "$PID" 2>/dev/null; then
    echo "Gateway process died during startup. First 50 lines of error:" >&2
    head -n 50 /tmp/gateway.log >&2
    exit 1
  fi
  
  if [ $tries -gt 60 ]; then
    echo "Gateway did not become live in time. Logs:" >&2
    tail -n 200 /tmp/gateway.log || true
    kill $PID || true
    exit 1
  fi
  sleep 2
done

echo "Live check OK"
curl -fsS "http://localhost:$PORT/q/health/live" | sed -e 's/.*/[health-live] &/'

kill $PID
wait $PID || true
echo "Stopped (PID=$PID)"

