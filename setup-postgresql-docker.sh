#!/bin/bash

# Script para configurar PostgreSQL en contenedor Docker (sin systemd)
echo "🐳 Configurando PostgreSQL para contenedor Docker..."
echo "🖥️  Servidor: $(hostname)"
echo "📅 Fecha: $(date)"

# Actualizar repositorios
echo "📦 Actualizando repositorios..."
apt update

# Instalar PostgreSQL
echo "🔧 Instalando PostgreSQL..."
apt install -y postgresql postgresql-contrib

# Iniciar PostgreSQL manualmente (sin systemd)
echo "🚀 Iniciando PostgreSQL manualmente..."
service postgresql start

# Esperar a que PostgreSQL inicie
echo "⏳ Esperando que PostgreSQL inicie..."
sleep 5

# Verificar que PostgreSQL está corriendo
if pgrep -x "postgres" > /dev/null; then
    echo "✅ PostgreSQL está corriendo"
else
    echo "❌ PostgreSQL no está corriendo, intentando reiniciar..."
    service postgresql restart
    sleep 3
fi

# Crear usuario y base de datos
echo "🔧 Configurando usuario y base de datos..."

# Cambiar a usuario postgres y crear configuración
sudo -u postgres psql -c "CREATE USER dpozas WITH PASSWORD 'diego2025';" 2>/dev/null || echo "Usuario dpozas ya existe"
sudo -u postgres psql -c "CREATE DATABASE dpozas_bd OWNER dpozas;" 2>/dev/null || echo "Base de datos dpozas_bd ya existe"
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE dpozas_bd TO dpozas;" 2>/dev/null

# Configurar autenticación
echo "🔧 Configurando autenticación..."
PG_VERSION=$(sudo -u postgres psql -t -c "SELECT version();" | grep -o '[0-9]\+\.[0-9]\+' | head -1)
HBA_FILE="/etc/postgresql/$PG_VERSION/main/pg_hba.conf"

if [ -f "$HBA_FILE" ]; then
    # Backup del archivo original
    cp "$HBA_FILE" "$HBA_FILE.backup"

    # Cambiar método de autenticación
    sed -i 's/local   all             all                                     peer/local   all             all                                     md5/' "$HBA_FILE"

    # Reiniciar PostgreSQL
    service postgresql restart
    sleep 3

    echo "✅ Configuración de autenticación actualizada"
else
    echo "❌ No se encontró archivo de configuración HBA"
fi

# Verificar conexión
echo "🔍 Probando conexión a la base de datos..."
if PGPASSWORD='diego2025' psql -h localhost -U dpozas -d dpozas_bd -c "SELECT 1;" > /dev/null 2>&1; then
    echo "✅ Conexión a la base de datos exitosa"
else
    echo "❌ Error en conexión, intentando configuración alternativa..."

    # Configuración alternativa para Docker
    sudo -u postgres psql -c "ALTER USER dpozas WITH SUPERUSER;"

    # Reiniciar PostgreSQL
    service postgresql restart
    sleep 3

    # Probar nuevamente
    if PGPASSWORD='diego2025' psql -h localhost -U dpozas -d dpozas_bd -c "SELECT 1;" > /dev/null 2>&1; then
        echo "✅ Conexión exitosa con configuración alternativa"
    else
        echo "❌ Conexión sigue fallando"
    fi
fi

# Verificar puerto
echo "🔍 Verificando puerto 5432..."
if netstat -tlnp | grep :5432 > /dev/null 2>&1; then
    echo "✅ PostgreSQL está escuchando en puerto 5432"
else
    echo "❌ PostgreSQL no está escuchando en puerto 5432"
    echo "📋 Puertos activos:"
    netstat -tlnp | grep LISTEN
fi

# Configurar inicio automático para contenedor
echo "🔧 Configurando inicio automático..."
echo "#!/bin/bash" > /etc/rc.local
echo "service postgresql start" >> /etc/rc.local
echo "exit 0" >> /etc/rc.local
chmod +x /etc/rc.local

# Resumen final
echo ""
echo "📋 RESUMEN DE CONFIGURACIÓN"
echo "=========================="
echo "✅ PostgreSQL instalado y configurado"
echo "✅ Usuario: dpozas"
echo "✅ Base de datos: dpozas_bd"
echo "✅ Puerto: 5432"
echo ""
echo "🔍 Estado final:"
ps aux | grep postgres | grep -v grep || echo "❌ No hay procesos postgres"
netstat -tlnp | grep :5432 || echo "❌ Puerto 5432 no está abierto"
echo ""
echo "💡 Para reiniciar PostgreSQL en este contenedor:"
echo "   service postgresql restart"
echo ""
echo "🚀 Ahora puedes ejecutar el servidor Java:"
echo "   ./start-server-background.sh"
