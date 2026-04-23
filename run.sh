#!/bin/bash

# Define colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}=======================================${NC}"
echo -e "${BLUE}    Starting DocuVerify Platform       ${NC}"
echo -e "${BLUE}=======================================${NC}"

echo -e "${GREEN}1. Checking prerequisites...${NC}"
# Quick check if psql and node are available (optional but helpful)
if ! command -v node &> /dev/null; then
    echo -e "${RED}Error: Node.js is not installed.${NC}"
    exit 1
fi

# If a backend/.env file exists, source it so env vars are available
if [ -f "backend/.env" ]; then
    echo -e "${BLUE}Loading backend/.env into environment...${NC}"
    set -a
    # shellcheck disable=SC1090
    . backend/.env
    set +a
fi

# Ensure JWT secret is available for local development
if [ -z "$JWT_SECRET" ]; then
    echo -e "${BLUE}No JWT_SECRET found. Generating a temporary secret for development...${NC}"
    if command -v openssl &> /dev/null; then
        export JWT_SECRET=$(openssl rand -base64 32)
    else
        export JWT_SECRET=$(head -c 32 /dev/urandom | base64)
    fi
    echo -e "${GREEN}JWT_SECRET set for this session (development only).${NC}"
fi

# Start Backend
echo -e "${GREEN}2. Starting Backend (Spring Boot)...${NC}"
cd backend || exit
# Using the bundled maven wrapper
./mvnw spring-boot:run > /dev/null 2>&1 &
BACKEND_PID=$!
cd ..

# Start Frontend
echo -e "${GREEN}3. Starting Frontend (Vite)...${NC}"
cd frontend || exit
# Start the frontend server
npm run dev > /dev/null 2>&1 &
FRONTEND_PID=$!
cd ..

echo -e "${BLUE}=======================================${NC}"
echo -e "${GREEN}✓ Services are booting up!${NC}"
echo -e "   - Frontend will be available at: http://localhost:5173"
echo -e "   - Backend is running on port 8080"
echo -e "${BLUE}=======================================${NC}"
echo -e "${RED}Press Ctrl+C to stop both services.${NC}"

# Trap SIGINT (Ctrl+C) to gracefully shut down both background processes
trap "echo -e '\n${BLUE}Shutting down DocuVerify services...${NC}'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0" SIGINT SIGTERM

# Wait indefinitely so the script doesn't exit until interrupted
wait $BACKEND_PID $FRONTEND_PID
