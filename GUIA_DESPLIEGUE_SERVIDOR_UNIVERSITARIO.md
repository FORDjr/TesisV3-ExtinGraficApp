# Guía de Despliegue en Servidor Universitario

## Credenciales del Servidor
- **IP**: 146.83.198.35
- **Puerto SSH**: 1608
- **Puerto Apache**: 1609
- **Usuario**: dpozas
- **Contraseña**: diego2025
- **Root**: pozas2025adm

## Credenciales de Base de Datos
- **Host**: pgsqltrans.face.ubiobio.cl
- **Usuario**: dpozas
- **Contraseña**: diego2025
- **Base de datos**: dpozas_bd

## Pasos para Desplegar el Servidor

### 1. Conectar al Servidor
```bash
ssh dpozas@146.83.198.35 -p 1608
```

### 2. Cambiar a Usuario Root
```bash
su
# Introducir contraseña: pozas2025adm
```

### 3. Instalar Java 11 (si no está instalado)
```bash
apt update
apt install openjdk-11-jdk -y
```

### 4. Verificar Java
```bash
java -version
```

### 5. Subir el Proyecto al Servidor
Puedes usar SCP o FileZilla para subir la carpeta `TesisV3` al servidor:
```bash
scp -P 1608 -r "F:\aProyectos UNIVERSIDAD\TesisV3" dpozas@146.83.198.35:~/
```

### 6. Ejecutar el Script de Despliegue
```bash
cd ~/
chmod +x TesisV3/deploy-server.sh
./TesisV3/deploy-server.sh
```

### 7. Verificar que el Servidor Funciona
El servidor estará disponible en: http://146.83.198.35:1609

### 8. Probar Endpoints
- **Health Check**: http://146.83.198.35:1609/health
- **Inventario**: http://146.83.198.35:1609/api/inventario
- **Auth**: http://146.83.198.35:1609/api/auth

## Configuración del Cliente (Aplicación Móvil)

La aplicación móvil ya está configurada para conectarse al servidor universitario:
- **URL Base**: http://146.83.198.35:1609
- **Archivo de configuración**: `business-app/lib/api-config.ts`

## Solución de Problemas

### Si el puerto 1609 está ocupado:
```bash
sudo lsof -i :1609
sudo kill -9 <PID>
```

### Si hay problemas de conexión a la base de datos:
1. Verificar que estés conectado a la VPN universitaria
2. Verificar las credenciales de la base de datos
3. Probar conexión manual:
```bash
psql -h pgsqltrans.face.ubiobio.cl -U dpozas -d dpozas_bd
```

### Para mantener el servidor corriendo en background:
```bash
nohup ./TesisV3/deploy-server.sh > server.log 2>&1 &
```

## Comandos Útiles

### Ver logs del servidor:
```bash
tail -f server.log
```

### Detener el servidor:
```bash
ps aux | grep java
kill <PID>
```

### Reiniciar el servidor:
```bash
./TesisV3/deploy-server.sh
```
