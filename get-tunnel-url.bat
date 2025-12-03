@echo off
echo === COMANDOS PARA SERVIDOR (copiar y pegar) ===
echo.
echo # Ver procesos cloudflared actuales
echo ps -eo pid,ppid,cmd ^| grep -i cloudflared ^| grep -v grep
echo.
echo # Extraer URL del proceso activo
echo PID=$(pgrep -n -f "cloudflared tunnel")
echo strings /proc/$PID/fd/1 2^>/dev/null ^| sed -n 's^|.*\(https://[A-Za-z0-9.-]*trycloudflare.com\).*^|\1^|p' ^| tail -1
echo strings /proc/$PID/fd/2 2^>/dev/null ^| sed -n 's^|.*\(https://[A-Za-z0-9.-]*trycloudflare.com\).*^|\1^|p' ^| tail -1
echo.
echo # Guardar URL detectada
echo URL=$( { strings /proc/$PID/fd/1 2^>/dev/null; strings /proc/$PID/fd/2 2^>/dev/null; } ^| sed -n 's^|.*\(https://[A-Za-z0-9.-]*trycloudflare.com\).*^|\1^|p' ^| tail -1 )
echo echo "$URL" ^| tee /var/log/cloudflared-current-url.txt
echo.
echo # Probar endpoint health
echo if [ -n "$URL" ]; then curl -I "$URL/health"; else echo "No se detectó URL"; fi
echo.
echo # Regenerar túnel completo (mata anteriores y crea nuevo)
echo pkill -f "cloudflared tunnel" 2^>/dev/null ^|^| true
echo nohup cloudflared tunnel --no-autoupdate --url http://127.0.0.1:9090 --loglevel info --logfile /var/log/cloudflared.log ^>/dev/null 2^>^&1 ^&
echo sleep 5
echo NEW_URL=$(grep -a -i trycloudflare /var/log/cloudflared.log ^| sed -n 's^|.*\(https://[A-Za-z0-9.-]*trycloudflare.com\).*^|\1^|p' ^| tail -1)
echo echo "Nueva URL: $NEW_URL"
echo echo "$NEW_URL" ^> /var/log/cloudflared-current-url.txt
echo curl -I "$NEW_URL/health" ^|^| echo "Health puede tardar"
echo.
echo # Crear script permanente
echo cat ^>/usr/local/bin/regen-tunnel.sh ^<^<'EOF'
echo #!/usr/bin/env bash
echo pkill -f "cloudflared tunnel" 2^>/dev/null ^|^| true
echo nohup cloudflared tunnel --no-autoupdate --url http://127.0.0.1:9090 --loglevel info --logfile /var/log/cloudflared.log ^>/dev/null 2^>^&1 ^&
echo sleep 5
echo URL=$(grep -a -i trycloudflare /var/log/cloudflared.log ^| sed -n 's^|.*\(https://[A-Za-z0-9.-]*trycloudflare.com\).*^|\1^|p' ^| tail -1)
echo echo "$URL" ^| tee /var/log/cloudflared-current-url.txt
echo EOF
echo chmod +x /usr/local/bin/regen-tunnel.sh
echo.
echo # Consultar URL guardada
echo cat /var/log/cloudflared-current-url.txt
echo.
echo # Cerrar túnel
echo pkill -f "cloudflared tunnel" ^|^| true
echo.
pause

