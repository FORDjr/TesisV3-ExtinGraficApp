@echo off
echo 🔧 Configurando firewall para servidor Python...
echo.

REM Verificar si se ejecuta como administrador
net session >nul 2>&1
if %errorLevel% NEQ 0 (
    echo ❌ Este script debe ejecutarse como ADMINISTRADOR
    echo 👉 Haz clic derecho y selecciona "Ejecutar como administrador"
    pause
    exit /b 1
)

echo ✅ Ejecutando como administrador...

REM Eliminar reglas existentes
echo 🗑️ Eliminando reglas existentes...
netsh advfirewall firewall delete rule name="Python 8090" >nul 2>&1
netsh advfirewall firewall delete rule name="Python Server" >nul 2>&1

REM Crear nuevas reglas
echo 🔥 Creando reglas de firewall...
netsh advfirewall firewall add rule name="Python 8090" dir=in action=allow protocol=TCP localport=8090
netsh advfirewall firewall add rule name="Python 8090" dir=out action=allow protocol=TCP localport=8090

REM Verificar que se crearon las reglas
echo 🔍 Verificando reglas creadas...
netsh advfirewall firewall show rule name="Python 8090" dir=in | findstr "Python 8090" >nul
if %errorLevel% EQU 0 (
    echo ✅ Reglas de firewall configuradas correctamente
) else (
    echo ❌ Error configurando firewall
)

echo.
echo 🚀 Ahora puedes ejecutar el servidor Python
echo 📱 URLs para tu celular:
echo    WiFi: http://192.168.1.24:8090
echo    Radmin VPN: http://26.36.148.66:8090
echo    OpenVPN: http://10.0.11.2:8090
echo.
pause
