#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"

JAVA_21_DEFAULT="/usr/lib/jvm/java-21-openjdk"
export JAVA_HOME="${JAVA_HOME:-$JAVA_21_DEFAULT}"
export PATH="$JAVA_HOME/bin:$PATH"

if [[ -f "$BACKEND_DIR/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "$BACKEND_DIR/.env"
  set +a
fi

echo "Using JAVA_HOME=$JAVA_HOME"
echo "Starting backend and frontend..."

(
  cd "$BACKEND_DIR"
  ./mvnw spring-boot:run
) &
BACKEND_PID=$!

(
  cd "$FRONTEND_DIR"
  npm run dev
) &
FRONTEND_PID=$!

cleanup() {
  echo "Stopping local services..."
  kill "$BACKEND_PID" "$FRONTEND_PID" 2>/dev/null || true
}

trap cleanup INT TERM EXIT

# If either process exits, stop the other.
wait -n "$BACKEND_PID" "$FRONTEND_PID"
