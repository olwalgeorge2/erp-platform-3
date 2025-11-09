#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(pwd)"
ALLOWLIST="${1:-scripts/ci/error-allowlist.txt}"

if ! command -v rg >/dev/null 2>&1; then
  echo "[log-gate] ripgrep (rg) not found; installing is recommended for speed." >&2
fi

echo "[log-gate] Scanning build outputs for unexpected errors..."

TMP_ALL=$(mktemp)
TMP_LEFT=$(mktemp)

# Collect matches across build outputs (errors and exceptions)
if command -v rg >/dev/null 2>&1; then
  rg -n "ERROR|Exception" --no-ignore --hidden --glob "**/build/**" >"$TMP_ALL" || true
else
  grep -RniE "ERROR|Exception" -- */build/ >"$TMP_ALL" || true
fi

if [[ ! -s "$TMP_ALL" ]]; then
  echo "[log-gate] No error-like lines found."
  rm -f "$TMP_ALL" "$TMP_LEFT"
  exit 0
fi

# Filter out allowlisted patterns (if file exists)
if [[ -f "$ALLOWLIST" ]]; then
  if command -v rg >/dev/null 2>&1; then
    rg -v -f "$ALLOWLIST" "$TMP_ALL" >"$TMP_LEFT" || true
  else
    grep -Ev -f "$ALLOWLIST" "$TMP_ALL" >"$TMP_LEFT" || true
  fi
else
  cp "$TMP_ALL" "$TMP_LEFT"
fi

if [[ -s "$TMP_LEFT" ]]; then
  echo "[log-gate] Unexpected error lines detected:" >&2
  sed -n '1,200p' "$TMP_LEFT" >&2
  rm -f "$TMP_ALL" "$TMP_LEFT"
  exit 1
fi

echo "[log-gate] All error lines are allowlisted."
rm -f "$TMP_ALL" "$TMP_LEFT"
exit 0

