#!/bin/bash

# Script de despliegue para el servidor universitario
# IP: 146.83.198.35
# Puerto: 1609

echo "🚀 Iniciando despliegue del servidor de inventario..."

# Configurar variables de entorno para producción
export env=production
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

# Mostrar información del sistema
echo "📋 Información del sistema:"
echo "Usuario: $(whoami)"
echo "Directorio actual: $(pwd)"
echo "Java version: $(java -version 2>&1 | head -1)"

# Verificar si el proyecto existe
if [ ! -d "TesisV3" ]; then
    echo "❌ Error: Directorio TesisV3 no encontrado"
    echo "Asegúrate de estar en el directorio correcto o de haber subido el proyecto"
    exit 1
fi

cd TesisV3

# Dar permisos de ejecución a gradlew
chmod +x gradlew

echo "🔧 Compilando el proyecto..."
./gradlew clean
./gradlew build

# Verificar si la compilación fue exitosa
if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa"
else
    echo "❌ Error en la compilación"
    exit 1
fi

echo "🌐 Iniciando servidor en modo producción..."
echo "🔗 El servidor estará disponible en: http://146.83.198.35:1609"
echo "🛡️ Configuración de base de datos: pgsqltrans.face.ubiobio.cl"

# Ejecutar el servidor
./gradlew :server:run
