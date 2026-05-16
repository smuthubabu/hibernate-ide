#!/bin/bash
# Starts backend (port 8080) and frontend dev server (port 3000) concurrently

ROOT="$(cd "$(dirname "$0")" && pwd)"

# ── DB2 Type 2 support ──────────────────────────────────────────────────────
# Source the DB2 instance profile so libdb2jcct2.so is on LD_LIBRARY_PATH.
# Override by setting DB2_PROFILE=/path/to/db2profile before running start.sh.
if [ -z "$DB2INSTANCE" ]; then
  if [ -n "$DB2_PROFILE" ] && [ -f "$DB2_PROFILE" ]; then
    # shellcheck source=/dev/null
    . "$DB2_PROFILE"
    echo "==> Sourced DB2 profile: $DB2_PROFILE"
  else
    for _p in \
      "/home/db2inst1/sqllib/db2profile" \
      "/home/db2inst2/sqllib/db2profile" \
      "/opt/ibm/db2/*/db2profile"
    do
      # expand glob
      for _f in $_p; do
        if [ -f "$_f" ]; then
          # shellcheck source=/dev/null
          . "$_f"
          echo "==> Sourced DB2 profile: $_f"
          break 2
        fi
      done
    done
  fi
fi
# ────────────────────────────────────────────────────────────────────────────

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
