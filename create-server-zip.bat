@echo off
echo 📦 Creando archivo ZIP optimizado (solo archivos necesarios para el servidor)...

set ZIP_FILE=TesisV3-server-only.zip

REM Crear ZIP solo con archivos necesarios para el servidor
powershell -Command "& { $files = @('server', 'shared', 'build.gradle.kts', 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat', 'gradle', 'start-server-simple.sh', 'setup-postgresql-docker.sh', 'setup-apache-proxy.sh'); Compress-Archive -Path $files -DestinationPath '%ZIP_FILE%' -Force }"

if exist "%ZIP_FILE%" (
    echo ✅ Archivo ZIP optimizado creado: %ZIP_FILE%
    echo 📏 Tamaño mucho menor - solo archivos del servidor
    echo.
    echo 📤 Ahora puedes subir este archivo ZIP al servidor usando:
    echo.
    echo 1. FileZilla SFTP:
    echo    Host: 146.83.198.35
    echo    Puerto: 1608
    echo    Usuario: dpozas
    echo    Contraseña: diego2025
    echo.
    echo 2. O usar SCP si tienes WSL:
    echo    wsl scp -P 1608 %ZIP_FILE% dpozas@146.83.198.35:~/
    echo.
    echo 📝 Después de subir, conecta al servidor y extrae:
    echo    ssh dpozas@146.83.198.35 -p 1608
    echo    unzip %ZIP_FILE%
    echo    chmod +x deploy-server.sh
    echo    ./deploy-server.sh
) else (
    echo ❌ Error creando el archivo ZIP optimizado
)

pause
