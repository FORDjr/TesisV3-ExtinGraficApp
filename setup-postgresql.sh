#!/bin/bash

# Script para verificar y configurar PostgreSQL en Ubuntu
echo "🔍 Verificando configuración de PostgreSQL..."

# Verificar si PostgreSQL está instalado
if ! command -v psql &> /dev/null; then
    echo "❌ PostgreSQL no está instalado"
    echo "💡 Instalando PostgreSQL..."
    sudo apt update
    sudo apt install -y postgresql postgresql-contrib

    # Iniciar el servicio
    sudo systemctl start postgresql
    sudo systemctl enable postgresql
    echo "✅ PostgreSQL instalado y iniciado"
else
    echo "✅ PostgreSQL está instalado"
fi

# Verificar si el servicio está corriendo
if sudo systemctl is-active --quiet postgresql; then
    echo "✅ PostgreSQL está corriendo"
else
    echo "❌ PostgreSQL no está corriendo, iniciando..."
    sudo systemctl start postgresql
fi

# Verificar si la base de datos existe
echo "🔍 Verificando base de datos dpozas_bd..."
if sudo -u postgres psql -lqt | cut -d \| -f 1 | grep -qw dpozas_bd; then
    echo "✅ Base de datos dpozas_bd existe"
else
    echo "❌ Base de datos dpozas_bd no existe"
    echo "💡 Creando base de datos y usuario..."

    # Crear usuario y base de datos
    sudo -u postgres psql -c "CREATE USER dpozas WITH PASSWORD 'diego2025';"
    sudo -u postgres psql -c "CREATE DATABASE dpozas_bd OWNER dpozas;"
    sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE dpozas_bd TO dpozas;"

    echo "✅ Base de datos y usuario creados"
fi

# Verificar conexión
echo "🔍 Probando conexión a la base de datos..."
if PGPASSWORD='diego2025' psql -h localhost -U dpozas -d dpozas_bd -c "SELECT 1;" &> /dev/null; then
    echo "✅ Conexión a la base de datos exitosa"
else
    echo "❌ Error en la conexión a la base de datos"
    echo "💡 Verificando configuración de PostgreSQL..."

    # Verificar configuración de autenticación
    PG_VERSION=$(sudo -u postgres psql -t -c "SELECT version();" | grep -o '[0-9]\+\.[0-9]\+' | head -1)
    HBA_FILE="/etc/postgresql/$PG_VERSION/main/pg_hba.conf"

    if [ -f "$HBA_FILE" ]; then
        echo "📁 Archivo de configuración: $HBA_FILE"
        echo "🔧 Configurando autenticación..."

        # Backup del archivo original
        sudo cp "$HBA_FILE" "$HBA_FILE.backup"

        # Cambiar método de autenticación local a md5
        sudo sed -i 's/local   all             all                                     peer/local   all             all                                     md5/' "$HBA_FILE"

        # Reiniciar PostgreSQL
        sudo systemctl restart postgresql

        echo "✅ Configuración actualizada"
    else
        echo "❌ No se encontró archivo de configuración"
    fi
fi

# Verificar puertos
echo "🔍 Verificando puertos..."
if netstat -tlnp | grep :5432 &> /dev/null; then
    echo "✅ PostgreSQL está escuchando en puerto 5432"
else
    echo "❌ PostgreSQL no está escuchando en puerto 5432"
fi

echo ""
echo "📋 Resumen de configuración:"
echo "   Base de datos: dpozas_bd"
echo "   Usuario: dpozas"
echo "   Host: localhost"
echo "   Puerto: 5432"
echo ""
echo "💡 Ahora puedes intentar ejecutar el servidor nuevamente"
