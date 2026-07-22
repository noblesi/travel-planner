#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

: "${ORACLE_URL:?ORACLE_URL is required}"
: "${ORACLE_USERNAME:?ORACLE_USERNAME is required}"
: "${ORACLE_PASSWORD:?ORACLE_PASSWORD is required}"

cd "$ROOT_DIR/backend"
exec ./gradlew bootRun
