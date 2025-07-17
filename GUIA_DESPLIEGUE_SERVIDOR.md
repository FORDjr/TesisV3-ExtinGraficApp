                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               # Guía de Despliegue del Servidor en Universidad del Bío-Bío

## Información del Servidor
- **IP:** 146.83.198.35
- **Puerto SSH:** 1608
- **Puerto Apache:** 1609
- **Usuario:** dpozas
- **Contraseña:** diego2025
- **Usuario root:** root
- **Contraseña root:** pozas2025adm

## Base de Datos PostgreSQL
- **Usuario:** dpozas
- **Contraseña:** diego2025
- **Base de datos:** dpozas_bd
- **URL:** pgsqltrans.face.ubiobio.cl

## Pasos para el Despliegue

### 1. Preparar el Proyecto Localmente
```bash
# Desde la carpeta del proyecto
cd server
./gradlew build
```

### 2. Crear el JAR ejecutable
```bash
./gradlew shadowJar
# O si no tienes shadowJar plugin:
./gradlew distTar
```

### 3. Conectar al Servidor (desde VPN si estás fuera de la universidad)
```bash
ssh -p 1608 dpozas@146.83.198.35
```

### 4. Cambiar a usuario root
```bash
su
# Contraseña: pozas2025adm
```

### 5. Instalar Java/JDK si no está instalado
```bash
# Verificar si Java está instalado
java -version

# Si no está instalado:
apt update
apt install openjdk-11-jdk
```

### 6. Crear directorio para la aplicación
```bash
mkdir -p /opt/thesis-server
cd /opt/thesis-server
```

### 7. Subir archivos al servidor
Desde tu máquina local:
```bash
# Subir solo la carpeta server
scp -P 1608 -r ./server dpozas@146.83.198.35:/tmp/

# Luego en el servidor, mover los archivos:
mv /tmp/server/* /opt/thesis-server/
```

### 8. Configurar la Base de Datos
Asegúrate de que tu aplicación use las credenciales correctas:
- Host: localhost o pgsqltrans.face.ubiobio.cl
- Puerto: 5432
- Base de datos: dpozas_bd
- Usuario: dpozas
- Contraseña: diego2025

### 9. Configurar el servicio systemd
Crear archivo de servicio:
```bash
nano /etc/systemd/system/thesis-server.service
```

Contenido del archivo:
```ini
[Unit]
Description=Thesis Server Application
After=network.target

[Service]
Type=simple
User=dpozas
WorkingDirectory=/opt/thesis-server
ExecStart=/usr/bin/java -jar /opt/thesis-server/build/libs/server-1.0.0-all.jar
Restart=always
RestartSec=10
Environment=JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64

[Install]
WantedBy=multi-user.target
```

### 10. Habilitar y iniciar el servicio
```bash
systemctl daemon-reload
systemctl enable thesis-server
systemctl start thesis-server
systemctl status thesis-server
```

### 11. Configurar el firewall (si es necesario)
```bash
# Permitir el puerto de tu aplicación (ejemplo: 8080)
ufw allow 8080
```

### 12. Verificar que el servidor esté funcionando
```bash
# Ver logs
journalctl -u thesis-server -f

# Verificar que esté escuchando en el puerto
netstat -tlnp | grep java
```

## Configuración de Apache (Puerto 1609)

Si necesitas que tu aplicación sea accesible a través de Apache:

### 1. Habilitar módulos necesarios
```bash
a2enmod proxy
a2enmod proxy_http
systemctl restart apache2
```

### 2. Configurar Virtual Host
```bash
nano /etc/apache2/sites-available/thesis-server.conf
```

Contenido:
```apache
<VirtualHost *:1609>
    ServerName 146.83.198.35
    
    ProxyPreserveHost On
    ProxyRequests Off
    ProxyPass / http://localhost:8080/
    ProxyPassReverse / http://localhost:8080/
    
    ErrorLog ${APACHE_LOG_DIR}/thesis-server-error.log
    CustomLog ${APACHE_LOG_DIR}/thesis-server-access.log combined
</VirtualHost>
```

### 3. Habilitar el sitio
```bash
a2ensite thesis-server
systemctl restart apache2
```

## Comandos Útiles

### Gestión del Servicio
```bash
# Iniciar
systemctl start thesis-server

# Detener
systemctl stop thesis-server

# Reiniciar
systemctl restart thesis-server

# Ver estado
systemctl status thesis-server

# Ver logs
journalctl -u thesis-server -f
```

### Monitoreo
```bash
# Ver procesos Java
ps aux | grep java

# Ver puertos abiertos
netstat -tlnp | grep :8080

# Ver uso de recursos
top
htop
```

## Notas Importantes

1. **VPN**: Recuerda conectarte a la VPN de la universidad si estás fuera de la red.
2. **Backups**: Mantén backups de tu aplicación y base de datos.
3. **Logs**: Los logs se guardan en `/var/log/syslog` y puedes verlos con `journalctl`.
4. **Actualizaciones**: Para actualizar, sube el nuevo JAR y reinicia el servicio.

## Troubleshooting

### Si el servicio no inicia:
1. Verifica los logs: `journalctl -u thesis-server -f`
2. Verifica que Java esté instalado: `java -version`
3. Verifica que el JAR exista: `ls -la /opt/thesis-server/build/libs/`
4. Verifica los permisos: `chown -R dpozas:dpozas /opt/thesis-server`

### Si no puedes conectar a la base de datos:
1. Verifica que PostgreSQL esté corriendo: `systemctl status postgresql`
2. Verifica la configuración de conexión en tu aplicación
3. Prueba la conexión: `psql -h pgsqltrans.face.ubiobio.cl -U dpozas -d dpozas_bd`
