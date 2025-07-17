#!/bin/bash

# Script para ejecutar el servidor de inventario en background
# Configuración para servidor universitario Ubuntu 20.04

echo "🚀 Iniciando servidor de inventario en background..."
echo "🖥️  Servidor: Ubuntu 20.04 (146.83.198.35:1609)"
echo "📅 Fecha: $(date)"

# Configurar variables de entorno
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH

# Ir al directorio del proyecto
cd /home/dpozas/TesisV3

# Verificar que el proyecto existe
if [ ! -f "gradlew" ]; then
    echo "❌ Error: No se encontró gradlew en el directorio actual"
    echo "📂 Directorio actual: $(pwd)"
    echo "📋 Archivos disponibles:"
    ls -la
    exit 1
fi

# Dar permisos de ejecución si es necesario
chmod +x gradlew

# Crear archivo PID para control del proceso
PID_FILE="/home/dpozas/TesisV3/server.pid"
LOG_FILE="/home/dpozas/TesisV3/server.log"

# Función para detener servidor anterior
stop_previous_server() {
    echo "🔍 Verificando si hay servidor anterior ejecutándose..."

    # Buscar y detener por PID guardado
    if [ -f "$PID_FILE" ]; then
        OLD_PID=$(cat "$PID_FILE")
        if kill -0 "$OLD_PID" 2>/dev/null; then
            echo "🛑 Deteniendo servidor anterior (PID: $OLD_PID)"
            kill -TERM "$OLD_PID"
            sleep 3
            if kill -0 "$OLD_PID" 2>/dev/null; then
                echo "⚠️  Forzando cierre del servidor anterior"
                kill -KILL "$OLD_PID"
            fi
        fi
        rm -f "$PID_FILE"
    fi

    # Buscar y detener procesos por nombre
    pkill -f "server:run" 2>/dev/null || true
    pkill -f "inventario" 2>/dev/null || true

    echo "✅ Limpieza de procesos anterior completada"
}

# Detener servidor anterior
stop_previous_server

# Verificar Java
echo "☕ Verificando Java..."
if ! java -version 2>&1 | grep -q "openjdk version"; then
    echo "❌ Error: Java no está instalado o no es la versión correcta"
    echo "💡 Instalando OpenJDK 17..."
    sudo apt update
    sudo apt install -y openjdk-17-jdk
fi

# Ejecutar servidor en background
echo "🔧 Iniciando servidor en background..."
echo "📄 Logs se guardarán en: $LOG_FILE"

# Usar nohup para ejecutar en background y redirigir salida
nohup ./gradlew :server:run --no-daemon > "$LOG_FILE" 2>&1 &

# Obtener PID del proceso
SERVER_PID=$!

# Guardar PID para control posterior
echo "$SERVER_PID" > "$PID_FILE"

# Esperar un momento para que inicie
echo "⏳ Esperando que el servidor inicie..."
sleep 10

# Verificar que el servidor inició correctamente
if kill -0 "$SERVER_PID" 2>/dev/null; then
    echo "✅ Servidor iniciado exitosamente"
    echo "📋 PID del servidor: $SERVER_PID"
    echo "📄 Logs en: $LOG_FILE"
    echo "🌐 Servidor disponible en: http://146.83.198.35:1609"
    echo "🔒 Acceso interno: http://localhost:8080"
    echo ""
    echo "📝 Comandos útiles:"
    echo "   Ver logs en tiempo real: tail -f $LOG_FILE"
    echo "   Detener servidor: kill $SERVER_PID"
    echo "   O usar: ./stop-server.sh"
    echo "   Ver estado: ps aux | grep java"
    echo "   Verificar puerto: netstat -tlnp | grep :8080"
    echo ""
    echo "🎯 El servidor seguirá ejecutándose aunque cierres la terminal"
    echo "💾 PID guardado en: $PID_FILE"
else
    echo "❌ Error: El servidor no pudo iniciarse"
    echo "📋 Verificando logs para más información:"
    if [ -f "$LOG_FILE" ]; then
        tail -20 "$LOG_FILE"
    fi
    exit 1
fi
