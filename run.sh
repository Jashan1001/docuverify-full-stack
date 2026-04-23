#!/usr/bin/env bash
set -euo pipefail

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
LOG_DIR="$ROOT_DIR/.logs"
BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"

BACKEND_PID=""
FRONTEND_PID=""

cleanup() {
    echo -e "\n${BLUE}Shutting down DocuVerify services...${NC}"
    if [[ -n "$BACKEND_PID" ]]; then kill "$BACKEND_PID" 2>/dev/null || true; fi
    if [[ -n "$FRONTEND_PID" ]]; then kill "$FRONTEND_PID" 2>/dev/null || true; fi
}

echo -e "${BLUE}=======================================${NC}"
echo -e "${BLUE}    Starting DocuVerify Platform       ${NC}"
echo -e "${BLUE}=======================================${NC}"

echo -e "${GREEN}1. Checking prerequisites...${NC}"
if ! command -v node >/dev/null 2>&1; then
    echo -e "${RED}Error: Node.js is not installed.${NC}"
    exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
    echo -e "${RED}Error: npm is not installed.${NC}"
    exit 1
fi

if ! command -v java >/dev/null 2>&1; then
    echo -e "${RED}Error: Java is not installed.${NC}"
    exit 1
fi

if [[ ! -x "$BACKEND_DIR/mvnw" ]]; then
    echo -e "${RED}Error: backend/mvnw missing or not executable.${NC}"
    exit 1
fi

if [[ -z "${JAVA_HOME:-}" && -d "/usr/lib/jvm/java-21-openjdk" ]]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk"
    export PATH="$JAVA_HOME/bin:$PATH"
fi

echo -e "${BLUE}Using $(java -version 2>&1 | head -n 1)${NC}"

if [[ -f "$BACKEND_DIR/.env" ]]; then
    echo -e "${BLUE}Loading backend/.env into environment...${NC}"
    set -a
    # shellcheck disable=SC1090
    source "$BACKEND_DIR/.env"
    set +a
fi

if [[ -z "${JWT_SECRET:-}" ]]; then
    echo -e "${BLUE}No JWT_SECRET found. Generating a temporary dev secret...${NC}"
    if command -v openssl >/dev/null 2>&1; then
        export JWT_SECRET="$(openssl rand -base64 32)"
    else
        export JWT_SECRET="$(head -c 32 /dev/urandom | base64)"
    fi
    echo -e "${GREEN}JWT_SECRET set for this session (development only).${NC}"
fi

mkdir -p "$LOG_DIR"
: > "$BACKEND_LOG"
: > "$FRONTEND_LOG"

trap cleanup INT TERM EXIT

echo -e "${GREEN}2. Starting Backend (Spring Boot)...${NC}"
(
    cd "$BACKEND_DIR"
    ./mvnw spring-boot:run >> "$BACKEND_LOG" 2>&1
) &
BACKEND_PID=$!

echo -e "${BLUE}Waiting for backend to become healthy...${NC}"
BACKEND_READY="false"
for _ in {1..60}; do
    if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
        echo -e "${RED}Backend exited during startup. See $BACKEND_LOG${NC}"
        tail -n 80 "$BACKEND_LOG" || true
        exit 1
    fi

    if command -v curl >/dev/null 2>&1 && curl -fs "http://localhost:8080/api/public/health" >/dev/null; then
        BACKEND_READY="true"
        break
    fi

    if command -v ss >/dev/null 2>&1 && ss -ltn | grep -q ':8080 '; then
        BACKEND_READY="true"
        break
    fi

    sleep 1
done

if [[ "$BACKEND_READY" != "true" ]]; then
    echo -e "${RED}Backend did not become ready in time. See $BACKEND_LOG${NC}"
    tail -n 80 "$BACKEND_LOG" || true
    exit 1
fi

echo -e "${GREEN}3. Starting Frontend (Vite)...${NC}"
(
    cd "$FRONTEND_DIR"
    npm run dev >> "$FRONTEND_LOG" 2>&1
) &
FRONTEND_PID=$!

echo -e "${BLUE}=======================================${NC}"
echo -e "${GREEN}✓ Services started${NC}"
echo -e "   - Frontend: http://localhost:5173"
echo -e "   - Backend:  http://localhost:8080"
echo -e "   - Backend log:  $BACKEND_LOG"
echo -e "   - Frontend log: $FRONTEND_LOG"
echo -e "${BLUE}=======================================${NC}"
echo -e "${RED}Press Ctrl+C to stop both services.${NC}"

wait -n "$BACKEND_PID" "$FRONTEND_PID"
