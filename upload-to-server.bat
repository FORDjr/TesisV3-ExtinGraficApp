@echo off
echo 🚀 Script de subida al servidor universitario
echo.

REM Configuración
set SERVER_IP=146.83.198.35
set SERVER_PORT=1608
set SERVER_USER=dpozas
set LOCAL_PATH=F:\aProyectos UNIVERSIDAD\TesisV3
set REMOTE_PATH=~/TesisV3

echo 📋 Configuración:
echo IP del servidor: %SERVER_IP%
echo Puerto SSH: %SERVER_PORT%
echo Usuario: %SERVER_USER%
echo Ruta local: %LOCAL_PATH%
echo Ruta remota: %REMOTE_PATH%
echo.

echo ⚠️  IMPORTANTE: Asegúrate de tener PuTTY/pscp instalado o usar WSL
echo.

echo 🔄 Subiendo proyecto al servidor...
echo Contraseña requerida: diego2025
echo.

REM Intentar con pscp (PuTTY)
where pscp >nul 2>nul
if %errorlevel% == 0 (
    echo Usando pscp...
    pscp -P %SERVER_PORT% -r "%LOCAL_PATH%" %SERVER_USER%@%SERVER_IP%:%REMOTE_PATH%
) else (
    echo pscp no encontrado. Usa uno de estos métodos alternativos:
    echo.
    echo 1. Instalar PuTTY y usar:
    echo    pscp -P %SERVER_PORT% -r "%LOCAL_PATH%" %SERVER_USER%@%SERVER_IP%:%REMOTE_PATH%
    echo.
    echo 2. Usar WSL:
    echo    wsl scp -P %SERVER_PORT% -r "%LOCAL_PATH%" %SERVER_USER%@%SERVER_IP%:%REMOTE_PATH%
    echo.
    echo 3. Usar FileZilla con configuración SFTP:
    echo    Host: %SERVER_IP%
    echo    Puerto: %SERVER_PORT%
    echo    Usuario: %SERVER_USER%
    echo    Contraseña: diego2025
    echo.
)

echo.
echo 📝 Después de subir el proyecto, conecta al servidor:
echo ssh %SERVER_USER%@%SERVER_IP% -p %SERVER_PORT%
echo.
echo 🚀 Y ejecuta el script de despliegue:
echo ./TesisV3/deploy-server.sh
echo.
pause
