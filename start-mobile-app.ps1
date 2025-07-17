# Script de PowerShell para probar la aplicación móvil con el servidor universitario
Write-Host "📱 Probando aplicación móvil con servidor universitario..." -ForegroundColor Green

# Variables
$SERVER_URL = "http://146.83.198.35:1609"
$API_CONFIG_FILE = "business-app\lib\api-config.ts"

Write-Host "🔧 Verificando configuración de la aplicación..." -ForegroundColor Yellow

# Verificar que el archivo de configuración existe
if (!(Test-Path $API_CONFIG_FILE)) {
    Write-Host "❌ Archivo de configuración no encontrado: $API_CONFIG_FILE" -ForegroundColor Red
    exit 1
}

# Mostrar configuración actual
Write-Host "📋 Configuración actual:" -ForegroundColor Blue
Get-Content $API_CONFIG_FILE | Select-String -Pattern "BASE_URL" -Context 3

Write-Host ""
Write-Host "🌐 Probando conectividad con el servidor..." -ForegroundColor Yellow

# Probar endpoint health
Write-Host "1. Probando /health..." -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$SERVER_URL/health" -TimeoutSec 10
    Write-Host "✅ Health check exitoso: $response" -ForegroundColor Green
} catch {
    Write-Host "❌ Health check falló: $($_.Exception.Message)" -ForegroundColor Red
}

# Probar endpoint inventario
Write-Host "2. Probando /api/inventario..." -ForegroundColor Cyan
try {
    $response = Invoke-RestMethod -Uri "$SERVER_URL/api/inventario" -TimeoutSec 10
    Write-Host "✅ Inventario accesible" -ForegroundColor Green
    Write-Host "Respuesta: $($response | ConvertTo-Json)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Inventario no accesible: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "🚀 Iniciando aplicación móvil..." -ForegroundColor Green
Write-Host "Asegúrate de que:" -ForegroundColor Yellow
Write-Host "1. El servidor esté ejecutándose en 146.83.198.35:1609" -ForegroundColor White
Write-Host "2. Estés conectado a la VPN universitaria" -ForegroundColor White
Write-Host "3. El emulador tenga acceso a internet" -ForegroundColor White

# Ir al directorio de la aplicación
Set-Location business-app

# Verificar si Node.js está instalado
if (!(Get-Command node -ErrorAction SilentlyContinue)) {
    Write-Host "❌ Node.js no está instalado. Instálalo desde https://nodejs.org/" -ForegroundColor Red
    exit 1
}

# Instalar dependencias si es necesario
if (!(Test-Path "node_modules")) {
    Write-Host "📦 Instalando dependencias..." -ForegroundColor Yellow
    npm install
}

# Iniciar la aplicación
Write-Host "🎯 Iniciando aplicación Next.js..." -ForegroundColor Green
npm run dev
