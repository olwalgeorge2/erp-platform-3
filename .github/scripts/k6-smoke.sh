#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")"/../.. && pwd)"
cd "$ROOT_DIR"

./gradlew :api-gateway:quarkusBuild -x test -Dquarkus.package.type=fast-jar

PORT=8080
export QUARKUS_HTTP_PORT=$PORT
export JWT_ENABLED=false

JAR="api-gateway/build/quarkus-app/quarkus-run.jar"
java -jar "$JAR" >/tmp/gateway-k6.log 2>&1 &
PID=$!
trap "kill $PID || true" EXIT

for i in {1..60}; do
  if curl -fsS "http://localhost:$PORT/q/health/live" >/dev/null; then break; fi
  sleep 1
done

echo "Running k6 smoke"
k6 run --summary-export /tmp/k6-summary.json -e GW_URL=http://localhost:$PORT load/k6/gateway-smoke.js | tee /tmp/k6-output.txt

echo "k6 summary written to /tmp/k6-summary.json"

kill $PID || true
wait $PID || true
