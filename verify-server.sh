#!/bin/bash

# Script para verificar el estado del servidor y la conectividad
echo "🔍 Verificando estado del servidor de inventario..."

# Variables de configuración
SERVER_HOST="146.83.198.35"
SERVER_PORT="1609"
DB_HOST="pgsqltrans.face.ubiobio.cl"
DB_PORT="5432"
DB_NAME="dpozas_bd"
DB_USER="dpozas"

echo "📋 Configuración del servidor:"
echo "Host: $SERVER_HOST"
echo "Puerto: $SERVER_PORT"
echo "Base de datos: $DB_HOST:$DB_PORT/$DB_NAME"
echo "Usuario DB: $DB_USER"
echo ""

# Verificar conectividad al servidor
echo "🌐 Verificando conectividad al servidor..."
if curl -s --connect-timeout 10 "http://$SERVER_HOST:$SERVER_PORT/health" > /dev/null; then
    echo "✅ Servidor accesible en http://$SERVER_HOST:$SERVER_PORT"
else
    echo "❌ No se puede acceder al servidor"
    echo "Posibles causas:"
    echo "1. El servidor no está ejecutándose"
    echo "2. Problemas de red/VPN"
    echo "3. Firewall bloqueando el puerto $SERVER_PORT"
fi

# Verificar endpoint health
echo ""
echo "🏥 Verificando endpoint /health..."
HEALTH_RESPONSE=$(curl -s --connect-timeout 10 "http://$SERVER_HOST:$SERVER_PORT/health")
if [ $? -eq 0 ] && [ ! -z "$HEALTH_RESPONSE" ]; then
    echo "✅ Health check exitoso: $HEALTH_RESPONSE"
else
    echo "❌ Health check falló"
fi

# Verificar endpoint inventario
echo ""
echo "📦 Verificando endpoint /api/inventario..."
INVENTARIO_RESPONSE=$(curl -s --connect-timeout 10 "http://$SERVER_HOST:$SERVER_PORT/api/inventario")
if [ $? -eq 0 ]; then
    echo "✅ Endpoint inventario accesible"
    echo "Respuesta: $INVENTARIO_RESPONSE"
else
    echo "❌ Endpoint inventario no accesible"
fi

# Verificar conectividad a la base de datos (requiere psql)
echo ""
echo "🗄️ Verificando conectividad a la base de datos..."
if command -v psql &> /dev/null; then
    if PGPASSWORD="diego2025" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "\dt" > /dev/null 2>&1; then
        echo "✅ Base de datos accesible"
        echo "Tablas en la base de datos:"
        PGPASSWORD="diego2025" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "\dt"
    else
        echo "❌ No se puede conectar a la base de datos"
        echo "Verifica:"
        echo "1. Conexión VPN activa"
        echo "2. Credenciales correctas"
        echo "3. Host de base de datos accesible"
    fi
else
    echo "⚠️ psql no está instalado, no se puede verificar la base de datos"
fi

echo ""
echo "📊 Verificación completa."
