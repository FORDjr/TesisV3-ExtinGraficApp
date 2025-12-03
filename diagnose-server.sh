#!/bin/bash

# Script de diagnóstico completo del servidor
echo "🔍 DIAGNÓSTICO COMPLETO DEL SERVIDOR"
echo "===================================="
echo "📅 Fecha: $(date)"
echo "🖥️  Servidor: $(hostname)"
echo ""

# 1. Verificar procesos
echo "1️⃣ VERIFICANDO PROCESOS DEL SERVIDOR"
echo "------------------------------------"
JAVA_PROCESSES=$(ps aux | grep java | grep -v grep)
if [ -n "$JAVA_PROCESSES" ]; then
    echo "✅ Procesos Java encontrados:"
    echo "$JAVA_PROCESSES"
else
    echo "❌ No se encontraron procesos Java"
fi

SERVER_PROCESSES=$(ps aux | grep "server:run" | grep -v grep)
if [ -n "$SERVER_PROCESSES" ]; then
    echo "✅ Proceso del servidor encontrado:"
    echo "$SERVER_PROCESSES"
else
    echo "❌ No se encontró proceso del servidor"
fi

# 2. Verificar archivo PID
echo ""
echo "2️⃣ VERIFICANDO ARCHIVO PID"
echo "-------------------------"
PID_FILE="/home/dpozas/TesisV3/server.pid"
if [ -f "$PID_FILE" ]; then
    SERVER_PID=$(cat "$PID_FILE")
    echo "📋 PID guardado: $SERVER_PID"
    if kill -0 "$SERVER_PID" 2>/dev/null; then
        echo "✅ Proceso con PID $SERVER_PID está corriendo"
    else
        echo "❌ Proceso con PID $SERVER_PID NO está corriendo"
    fi
else
    echo "❌ No se encontró archivo PID"
fi

# 3. Verificar puertos
echo ""
echo "3️⃣ VERIFICANDO PUERTOS"
echo "---------------------"
if netstat -tlnp | grep :8080 > /dev/null; then
    echo "✅ Puerto 8080 (interno) está abierto:"
    netstat -tlnp | grep :8080
else
    echo "❌ Puerto 8080 (interno) NO está abierto"
fi

if netstat -tlnp | grep :1609 > /dev/null; then
    echo "✅ Puerto 1609 (Apache) está abierto:"
    netstat -tlnp | grep :1609
else
    echo "❌ Puerto 1609 (Apache) NO está abierto"
fi

# 4. Verificar PostgreSQL
echo ""
echo "4️⃣ VERIFICANDO POSTGRESQL"
echo "------------------------"
if sudo systemctl is-active --quiet postgresql; then
    echo "✅ PostgreSQL está corriendo"
else
    echo "❌ PostgreSQL NO está corriendo"
fi

if PGPASSWORD='diego2025' psql -h localhost -U dpozas -d dpozas_bd -c "SELECT 1;" > /dev/null 2>&1; then
    echo "✅ Conexión a base de datos exitosa"
else
    echo "❌ Error en conexión a base de datos"
fi

# 5. Verificar logs
echo ""
echo "5️⃣ VERIFICANDO LOGS"
echo "------------------"
LOG_FILE="/home/dpozas/TesisV3/server.log"
if [ -f "$LOG_FILE" ]; then
    echo "📄 Archivo de logs encontrado"
    echo "📏 Tamaño: $(du -h "$LOG_FILE" | cut -f1)"
    echo "🕒 Última modificación: $(stat -c %y "$LOG_FILE")"
    echo ""
    echo "📋 Últimas 10 líneas del log:"
    tail -10 "$LOG_FILE"
else
    echo "❌ No se encontró archivo de logs"
fi

# 6. Verificar conectividad
echo ""
echo "6️⃣ VERIFICANDO CONECTIVIDAD"
echo "--------------------------"
if curl -s http://localhost:8080 > /dev/null; then
    echo "✅ Servidor responde en localhost:8080"
else
    echo "❌ Servidor NO responde en localhost:8080"
fi

# 7. Verificar archivos del proyecto
echo ""
echo "7️⃣ VERIFICANDO ARCHIVOS DEL PROYECTO"
echo "-----------------------------------"
cd /home/dpozas/TesisV3
if [ -f "gradlew" ]; then
    echo "✅ gradlew encontrado"
else
    echo "❌ gradlew NO encontrado"
fi

if [ -d "server" ]; then
    echo "✅ Directorio server encontrado"
else
    echo "❌ Directorio server NO encontrado"
fi

# 8. Resumen y recomendaciones
echo ""
echo "8️⃣ RESUMEN Y RECOMENDACIONES"
echo "============================="

if [ -n "$JAVA_PROCESSES" ] && netstat -tlnp | grep :8080 > /dev/null; then
    echo "✅ EL SERVIDOR PARECE ESTAR CORRIENDO CORRECTAMENTE"
    echo "🌐 Deberías poder acceder desde:"
    echo "   - Interno: http://localhost:8080"
    echo "   - Externo: http://146.83.198.35:1609"
else
    echo "❌ EL SERVIDOR NO ESTÁ CORRIENDO CORRECTAMENTE"
    echo "💡 Recomendaciones:"
    echo "   1. Ejecutar: ./start-server-background.sh"
    echo "   2. Si falla, verificar: ./setup-postgresql.sh"
    echo "   3. Ver logs: tail -f server.log"
fi

echo ""
echo "🔄 Para ejecutar este diagnóstico nuevamente: ./diagnose-server.sh"
