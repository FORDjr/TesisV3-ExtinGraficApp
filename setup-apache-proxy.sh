#!/bin/bash

# Script para configurar Apache como proxy reverso en puerto 80
echo "🌐 Configurando Apache como proxy reverso..."
echo "🎯 Redirigir puerto 80 → 8080 (puerto por defecto que funciona)"

# Detener Apache si está corriendo
echo "🛑 Deteniendo Apache actual..."
service apache2 stop 2>/dev/null || true

# Instalar Apache si no está instalado
if ! command -v apache2 &> /dev/null; then
    echo "📦 Instalando Apache..."
    apt update
    apt install -y apache2
else
    echo "✅ Apache ya está instalado"
fi

# Habilitar módulos necesarios
echo "🔧 Habilitando módulos de Apache..."
a2enmod proxy
a2enmod proxy_http
a2enmod proxy_balancer
a2enmod lbmethod_byrequests
a2enmod headers

# Deshabilitar sitio por defecto
echo "🔧 Deshabilitando sitio por defecto..."
a2dissite 000-default.conf 2>/dev/null || true

# Crear configuración del sitio para puerto 80
echo "📝 Creando configuración del sitio para puerto 80..."
cat > /etc/apache2/sites-available/java-proxy.conf << 'EOF'
<VirtualHost *:80>
    ServerName 146.83.198.35
    ServerAlias localhost
    DocumentRoot /var/www/html

    # Configuración del proxy para redirigir al servidor Java
    ProxyPreserveHost On
    ProxyRequests Off

    # Redirigir todo el tráfico al servidor Java en puerto 8080
    ProxyPass / http://localhost:8080/
    ProxyPassReverse / http://localhost:8080/

    # Headers para mejor compatibilidad
    Header always set Access-Control-Allow-Origin "*"
    Header always set Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS"
    Header always set Access-Control-Allow-Headers "Content-Type, Authorization, X-Requested-With"

    # Manejo de OPTIONS para CORS
    <Location />
        # Responder a preflight OPTIONS
        SetEnvIf Request_Method OPTIONS cors_method
        Header always set Access-Control-Allow-Origin "*" env=cors_method
        Header always set Access-Control-Allow-Methods "GET, POST, PUT, DELETE, OPTIONS" env=cors_method
        Header always set Access-Control-Allow-Headers "Content-Type, Authorization, X-Requested-With" env=cors_method
        Header always set Access-Control-Max-Age "3600" env=cors_method
    </Location>

    # Logs
    ErrorLog ${APACHE_LOG_DIR}/java-proxy_error.log
    CustomLog ${APACHE_LOG_DIR}/java-proxy_access.log combined
</VirtualHost>
EOF

# Habilitar el sitio
echo "✅ Habilitando sitio en puerto 80..."
a2ensite java-proxy.conf

# Asegurar que Apache escuche en puerto 80
echo "🔧 Configurando Apache para puerto 80..."
echo "Listen 80" > /etc/apache2/ports.conf

# Reiniciar Apache
echo "🔄 Reiniciando Apache..."
service apache2 restart

# Esperar a que Apache inicie
sleep 3

# Verificar configuración
echo "🔍 Verificando configuración..."
if pgrep apache2 > /dev/null; then
    echo "✅ Apache está corriendo"
else
    echo "❌ Apache no está corriendo"
    echo "📋 Intentando iniciar Apache..."
    service apache2 start
    sleep 2
fi

# Verificar puertos
echo "🔍 Verificando puertos..."
echo "Puerto 80 (Apache):"
netstat -tlnp | grep :80 || echo "❌ Puerto 80 no está abierto"

echo "Puerto 8080 (Java):"
netstat -tlnp | grep :8080 || echo "❌ Puerto 8080 no está abierto"

# Probar conectividad
echo "🔍 Probando conectividad..."
echo "Prueba servidor Java (puerto 8080):"
curl -s http://localhost:8080 > /dev/null && echo "✅ Java responde en 8080" || echo "❌ Java no responde en 8080"

echo "Prueba Apache (puerto 80):"
curl -s http://localhost:80 > /dev/null && echo "✅ Apache responde en 80" || echo "❌ Apache no responde en 80"

# Verificar configuración de Apache
echo "🔍 Verificando configuración de Apache..."
apache2ctl configtest

echo ""
echo "📋 RESUMEN DE CONFIGURACIÓN"
echo "=========================="
echo "✅ Apache configurado como proxy reverso en puerto 80"
echo "✅ Puerto 80 → Puerto 8080"
echo "✅ Headers CORS configurados"
echo "✅ Sitio por defecto deshabilitado"
echo ""
echo "🌐 Acceso externo: http://146.83.198.35"
echo "🔒 Acceso interno Java: http://localhost:8080"
echo "🔒 Acceso interno Apache: http://localhost:80"
echo ""
echo "📝 Comandos útiles:"
echo "   Reiniciar Apache: service apache2 restart"
echo "   Ver logs Apache: tail -f /var/log/apache2/java-proxy_access.log"
echo "   Ver errores Apache: tail -f /var/log/apache2/java-proxy_error.log"
echo "   Verificar configuración: apache2ctl configtest"
echo ""
echo "🎯 Ahora deberías poder acceder desde:"
echo "   - Postman: http://146.83.198.35"
echo "   - Browser: http://146.83.198.35"
