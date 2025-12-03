#!/bin/bash

# Script para probar la aplicación móvil con el servidor universitario
echo "📱 Probando aplicación móvil con servidor universitario..."

# Variables
SERVER_URL="http://146.83.198.35:1609"
API_CONFIG_FILE="business-app/lib/api-config.ts"

echo "🔧 Verificando configuración de la aplicación..."

# Verificar que el archivo de configuración existe
if [ ! -f "$API_CONFIG_FILE" ]; then
    echo "❌ Archivo de configuración no encontrado: $API_CONFIG_FILE"
    exit 1
fi

# Mostrar configuración actual
echo "📋 Configuración actual:"
grep -A 5 -B 5 "BASE_URL" "$API_CONFIG_FILE"

echo ""
echo "🌐 Probando conectividad con el servidor..."

# Probar endpoint health
echo "1. Probando /health..."
curl -s --connect-timeout 10 "$SERVER_URL/health" || echo "❌ Health check falló"

# Probar endpoint inventario
echo "2. Probando /api/inventario..."
curl -s --connect-timeout 10 "$SERVER_URL/api/inventario" || echo "❌ Inventario no accesible"

echo ""
echo "🚀 Iniciando aplicación móvil..."
echo "Asegúrate de que:"
echo "1. El servidor esté ejecutándose en 146.83.198.35:1609"
echo "2. Estés conectado a la VPN universitaria"
echo "3. El emulador tenga acceso a internet"

# Ir al directorio de la aplicación
cd business-app

# Instalar dependencias si es necesario
if [ ! -d "node_modules" ]; then
    echo "📦 Instalando dependencias..."
    npm install
fi

# Iniciar la aplicación
echo "🎯 Iniciando aplicación Next.js..."
npm run dev
