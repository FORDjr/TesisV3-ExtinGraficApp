#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [ -f .env ]; then
  echo "[start] Cargando variables de .env"
  set -a; . ./.env; set +a
fi
echo "[start] Parando instancia previa (si existe)"
pkill -f server.jar 2>/dev/null || true
echo "[start] Iniciando server.jar en puerto ${SERVER_PORT:-desconocido}"
nohup java -jar server.jar > app.log 2>&1 &
sleep 2
PID=$(pgrep -f server.jar || true)
if [ -n "$PID" ]; then
  echo "[start] Servidor iniciado con PID $PID"
else
  echo "[start] ERROR: No se pudo iniciar (ver app.log)" >&2
  exit 1
fi

