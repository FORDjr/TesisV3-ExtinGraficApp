#!/bin/bash

# Script para detener el servidor de inventario en Ubuntu
echo "🛑 Deteniendo servidor de inventario..."

PID_FILE="/home/dpozas/TesisV3/server.pid"
LOG_FILE="/home/dpozas/TesisV3/server.log"

# Función para detener servidor por PID
stop_by_pid() {
    if [ -f "$PID_FILE" ]; then
        SERVER_PID=$(cat "$PID_FILE")
        if kill -0 "$SERVER_PID" 2>/dev/null; then
            echo "🔄 Deteniendo servidor (PID: $SERVER_PID)..."
            kill -TERM "$SERVER_PID"

            # Esperar a que termine graciosamente
            for i in {1..10}; do
                if ! kill -0 "$SERVER_PID" 2>/dev/null; then
                    echo "✅ Servidor detenido exitosamente"
                    rm -f "$PID_FILE"
                    return 0
                fi
                sleep 1
            done

            # Si no termina, forzar cierre
            echo "⚠️  Forzando cierre del servidor..."
            kill -KILL "$SERVER_PID" 2>/dev/null
            rm -f "$PID_FILE"
            echo "✅ Servidor detenido (forzado)"
        else
            echo "❌ El proceso con PID $SERVER_PID ya no existe"
            rm -f "$PID_FILE"
        fi
    else
        echo "❌ No se encontró archivo PID: $PID_FILE"
        return 1
    fi
}

# Función para buscar y detener procesos por nombre
stop_by_name() {
    echo "🔍 Buscando procesos del servidor..."

    # Buscar procesos Java relacionados con el servidor
    JAVA_PIDS=$(pgrep -f "server:run" 2>/dev/null)
    if [ -n "$JAVA_PIDS" ]; then
        echo "🛑 Deteniendo procesos Java del servidor..."
        for pid in $JAVA_PIDS; do
            echo "   Deteniendo PID: $pid"
            kill -TERM "$pid" 2>/dev/null
        done
        sleep 3

        # Verificar si siguen ejecutándose
        JAVA_PIDS=$(pgrep -f "server:run" 2>/dev/null)
        if [ -n "$JAVA_PIDS" ]; then
            echo "⚠️  Forzando cierre de procesos restantes..."
            for pid in $JAVA_PIDS; do
                kill -KILL "$pid" 2>/dev/null
            done
        fi
        echo "✅ Procesos Java detenidos"
    else
        echo "❌ No se encontraron procesos Java del servidor"
    fi
}

# Intentar detener por PID primero
if ! stop_by_pid; then
    # Si no hay archivo PID, buscar por nombre
    stop_by_name
fi

# Verificar que no queden procesos
echo "🔍 Verificación final..."
REMAINING=$(pgrep -f "server:run" 2>/dev/null)
if [ -n "$REMAINING" ]; then
    echo "⚠️  Aún hay procesos ejecutándose: $REMAINING"
else
    echo "✅ No hay procesos del servidor ejecutándose"
fi

# Mostrar información final
echo ""
echo "📋 Estado final:"
echo "   Archivo PID: $([ -f "$PID_FILE" ] && echo "No existe" || echo "Existe")"
echo "   Logs disponibles en: $LOG_FILE"
echo "   Verificar puerto: netstat -tlnp | grep :8080"
echo ""
echo "💡 Para reiniciar el servidor usa: ./start-server-background.sh"
