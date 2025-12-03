@echo off
echo 📦 Creando archivo ZIP del proyecto...

set PROJECT_DIR=F:\aProyectos UNIVERSIDAD\TesisV3
set ZIP_FILE=TesisV3-proyecto.zip

REM Crear ZIP usando PowerShell
powershell -Command "Compress-Archive -Path '%PROJECT_DIR%' -DestinationPath '%ZIP_FILE%' -Force"

if exist "%ZIP_FILE%" (
    echo ✅ Archivo ZIP creado: %ZIP_FILE%
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
    echo    unzip TesisV3-proyecto.zip
    echo    chmod +x TesisV3/deploy-server.sh
    echo    ./TesisV3/deploy-server.sh
) else (
    echo ❌ Error creando el archivo ZIP
)

pause
