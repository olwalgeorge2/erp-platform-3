#!/usr/bin/env bash
set -euo pipefail

# Generate a short-lived RS256 JWT for local dev.
# - Generates RSA keypair on first run under scripts/keys/
# - Emits token to stdout and writes to scripts/tokens/dev.jwt

ROOT_DIR="$(cd "$(dirname "$0")"/.. && pwd)"
KEYS_DIR="$ROOT_DIR/scripts/keys"
TOKENS_DIR="$ROOT_DIR/scripts/tokens"
PRIV="$KEYS_DIR/dev-jwt-private.pem"
PUB="$KEYS_DIR/dev-jwt-public.pem"
OUT="$TOKENS_DIR/dev.jwt"
GATEWAY_PUB_CLASSPATH="$ROOT_DIR/api-gateway/src/main/resources/keys/dev-jwt-public.pem"

SUBJECT="${1:-dev-user}"
ROLES_CSV="${2:-}"
ISSUER="${3:-erp-platform-dev}"
MINUTES="${4:-5}"

command -v openssl >/dev/null 2>&1 || { echo "openssl is required"; exit 1; }

mkdir -p "$KEYS_DIR" "$TOKENS_DIR"
if [ ! -f "$PRIV" ]; then
  echo "[dev-jwt] Generating RSA keypair under $KEYS_DIR"
  openssl genrsa -out "$PRIV" 2048 >/dev/null
  openssl rsa -in "$PRIV" -pubout -out "$PUB" >/dev/null
fi

now=$(date -u +%s)
exp=$(( now + (60 * MINUTES) ))

header='{"alg":"RS256","typ":"JWT"}'
payload_base='{'
payload_base+="\"iss\":\"$ISSUER\",\"sub\":\"$SUBJECT\",\"iat\":$now,\"exp\":$exp"

if [ -n "$ROLES_CSV" ]; then
  # Convert CSV to JSON array
  IFS=',' read -r -a arr <<< "$ROLES_CSV"
  roles_json="[\"${arr[*]//,/\",\"}\"]"
  payload_base+=",\"roles\":$roles_json"
fi
payload_base+='}'

b64url() {
  # base64url encode stdin
  openssl base64 -A | tr '+/' '-_' | tr -d '='
}

hdr_b64=$(printf '%s' "$header" | b64url)
pld_b64=$(printf '%s' "$payload_base" | b64url)
input="$hdr_b64.$pld_b64"

sig=$(printf '%s' "$input" | openssl dgst -sha256 -sign "$PRIV" -binary | openssl base64 -A | tr '+/' '-_' | tr -d '=')
token="$input.$sig"

printf '%s' "$token" > "$OUT"
echo "[dev-jwt] Token written to $OUT"
echo "$token"

# Copy public key into gateway resources for classpath resolution
if ! cp "$PUB" "$GATEWAY_PUB_CLASSPATH" 2>/dev/null; then
  echo "[dev-jwt] Warning: could not copy public key to $GATEWAY_PUB_CLASSPATH"
else
  echo "[dev-jwt] Public key copied to $GATEWAY_PUB_CLASSPATH"
fi
