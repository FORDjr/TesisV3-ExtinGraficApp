#!/bin/bash

# Script simple para iniciar el servidor (se detiene al cerrar terminal)
echo "🚀 Iniciando servidor de inventario..."
echo "📅 Fecha: $(date)"

# Configurar variables de entorno para producción
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
export env=production

echo "🔧 Configuración:"
echo "   Environment: $env"
echo "   Base de datos: dpozas_bd"
echo "   Puerto: 8080"

# Ir al directorio del proyecto
cd /home/dpozas/TesisV3

# Verificar que el proyecto existe
if [ ! -f "gradlew" ]; then
    echo "❌ Error: No se encontró gradlew"
    exit 1
fi

# Dar permisos de ejecución
chmod +x gradlew

echo "🔧 Iniciando servidor..."
echo "💡 Para detener el servidor, presiona Ctrl+C"
echo "🌐 Servidor estará disponible en: http://146.83.198.35"
echo ""

# Ejecutar servidor (se detiene al cerrar terminal)
./gradlew :server:run --no-daemon
