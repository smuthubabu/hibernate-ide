#!/bin/bash
# Starts backend (port 8080) and frontend dev server (port 3000) concurrently

ROOT="$(cd "$(dirname "$0")" && pwd)"

echo "==> Starting Hibernate IDE Backend (port 8080)..."
cd "$ROOT/backend"
mvn spring-boot:run -q &
BACKEND_PID=$!

echo "==> Starting Hibernate IDE Frontend (port 3000)..."
cd "$ROOT/frontend"
npm start &
FRONTEND_PID=$!

echo ""
echo "  Backend PID : $BACKEND_PID"
echo "  Frontend PID: $FRONTEND_PID"
echo ""
echo "  Open http://localhost:3000 in your browser"
echo "  Press Ctrl+C to stop both servers"
echo ""

trap "echo 'Stopping...'; kill $BACKEND_PID $FRONTEND_PID 2>/dev/null; exit 0" INT TERM
wait
