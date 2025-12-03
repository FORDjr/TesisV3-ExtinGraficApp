#!/bin/bash

# Script para probar diferentes puertos desde el contenedor
echo "🔍 Probando conectividad de puertos desde el servidor..."

PUERTOS=(80 443 1609 1610 8080 8081 3000)

for puerto in "${PUERTOS[@]}"; do
    echo "Probando puerto $puerto..."

    # Cambiar puerto en Constants.kt
    sed -i "s/const val PRODUCTION_PORT = [0-9]*/const val PRODUCTION_PORT = $puerto/" server/src/main/kotlin/org/example/project/Constants.kt

    # Limpiar y compilar
    ./gradlew :server:clean :server:build --no-daemon --quiet

    if [ $? -eq 0 ]; then
        echo "✅ Compilación exitosa para puerto $puerto"

        # Ejecutar servidor en background por 10 segundos
        timeout 10s ./gradlew :server:run --no-daemon &
        SERVER_PID=$!

        # Esperar un momento para que inicie
        sleep 5

        # Probar conectividad local
        if curl -s --connect-timeout 5 http://localhost:$puerto/health > /dev/null; then
            echo "🌐 Puerto $puerto accesible localmente"

            # Probar desde el host (esto requiere que pruebes desde fuera)
            echo "🔗 Prueba desde tu PC: http://146.83.198.35:$puerto/health"

            # Matar el servidor
            kill $SERVER_PID 2>/dev/null
            break
        else
            echo "❌ Puerto $puerto no accesible"
            kill $SERVER_PID 2>/dev/null
        fi
    else
        echo "❌ Error compilando para puerto $puerto"
    fi

    echo "---"
done

echo "✅ Prueba completa"
