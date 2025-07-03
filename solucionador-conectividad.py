#!/usr/bin/env python3
"""
Solucionador de conectividad celular - PC
Diagnóstico avanzado y soluciones automáticas
"""
import subprocess
import socket
import time
import threading
import webbrowser
import os

def ejecutar_comando(comando):
    """Ejecuta un comando y devuelve el resultado"""
    try:
        result = subprocess.run(comando, shell=True, capture_output=True, text=True)
        return result.stdout, result.stderr, result.returncode
    except Exception as e:
        return "", str(e), 1

def obtener_gateway():
    """Obtiene la IP del gateway/router"""
    stdout, _, _ = ejecutar_comando("ipconfig | findstr 'Puerta de enlace predeterminada'")
    if stdout:
        lines = stdout.split('\n')
        for line in lines:
            if '192.168.' in line:
                parts = line.split(':')
                if len(parts) > 1:
                    ip = parts[1].strip()
                    if ip and ip != '':
                        return ip
    return "192.168.1.1"  # Gateway común por defecto

def verificar_ping_gateway():
    """Verifica si se puede hacer ping al gateway"""
    gateway = obtener_gateway()
    print(f"🌐 Probando conectividad al router ({gateway})...")

    stdout, _, code = ejecutar_comando(f"ping -n 2 {gateway}")
    if code == 0 and "bytes=" in stdout:
        print(f"✅ Conectividad al router OK")
        return True, gateway
    else:
        print(f"❌ No se puede conectar al router")
        return False, gateway

def crear_configuracion_router(gateway):
    """Crea un archivo con instrucciones para configurar el router"""
    contenido = f"""
🔧 INSTRUCCIONES PARA CONFIGURAR TU ROUTER
============================================

1. ACCEDER AL ROUTER:
   • Abre un navegador en tu PC
   • Ve a: http://{gateway}
   • Usuario común: admin / Password común: admin (o ver etiqueta del router)

2. BUSCAR CONFIGURACIÓN INALÁMBRICA:
   • WiFi Settings / Configuración WiFi
   • Wireless Settings / Ajustes Inalámbricos
   • Advanced / Avanzado

3. DESACTIVAR AISLAMIENTO:
   Busca y DESACTIVA estas opciones:
   ✗ AP Isolation (Aislamiento AP)
   ✗ Client Isolation (Aislamiento de Cliente)
   ✗ Station Isolation
   ✗ Wireless Isolation
   ✗ Device Isolation (Aislamiento de Dispositivos)
   ✗ Inter-Device Communication (Comunicación entre Dispositivos)

4. GUARDAR Y REINICIAR:
   • Save / Guardar configuración
   • Restart / Reiniciar router
   • Esperar 2-3 minutos

5. PROBAR CONEXIÓN:
   • Desde tu celular: http://192.168.1.24:8090

ROUTERS COMUNES:
- TP-Link: Advanced → Wireless → Advanced
- Netgear: WiFi Settings → Access Control
- Linksys: WiFi Settings → WiFi Access Control
- ASUS: Adaptive QoS → Traditional QoS → Bandwidth Monitor
- Huawei: WiFi → WiFi Security
"""

    with open("configuracion-router.txt", "w", encoding="utf-8") as f:
        f.write(contenido)

    print(f"📝 Instrucciones guardadas en: configuracion-router.txt")
    return "configuracion-router.txt"

def intentar_solucion_windows():
    """Intenta soluciones automáticas en Windows"""
    print("\n🔧 INTENTANDO SOLUCIONES AUTOMÁTICAS:")
    print("=" * 50)

    # 1. Verificar y configurar perfil de red
    print("1️⃣ Configurando perfil de red como privada...")
    cmd1 = 'powershell -Command "Get-NetConnectionProfile | Set-NetConnectionProfile -NetworkCategory Private"'
    stdout, stderr, code = ejecutar_comando(cmd1)
    if code == 0:
        print("✅ Perfil de red configurado como privada")
    else:
        print("❌ Error configurando perfil de red")

    # 2. Habilitar descubrimiento de red
    print("2️⃣ Habilitando descubrimiento de red...")
    cmd2 = 'netsh advfirewall firewall set rule group="Network Discovery" new enable=Yes'
    stdout, stderr, code = ejecutar_comando(cmd2)
    if code == 0:
        print("✅ Descubrimiento de red habilitado")
    else:
        print("❌ Error habilitando descubrimiento de red")

    # 3. Habilitar compartir archivos e impresoras
    print("3️⃣ Habilitando compartir archivos...")
    cmd3 = 'netsh advfirewall firewall set rule group="File and Printer Sharing" new enable=Yes'
    stdout, stderr, code = ejecutar_comando(cmd3)
    if code == 0:
        print("✅ Compartir archivos habilitado")
    else:
        print("❌ Error habilitando compartir archivos")

    # 4. Verificar regla específica del puerto 8090
    print("4️⃣ Verificando regla de firewall puerto 8090...")
    cmd4 = 'netsh advfirewall firewall show rule name="Python 8090" dir=in'
    stdout, stderr, code = ejecutar_comando(cmd4)
    if "Python 8090" in stdout:
        print("✅ Regla de firewall existente")
    else:
        print("🔧 Creando regla de firewall...")
        cmd4b = 'netsh advfirewall firewall add rule name="Python 8090" dir=in action=allow protocol=TCP localport=8090'
        ejecutar_comando(cmd4b)
        print("✅ Regla de firewall creada")

def crear_servidor_test():
    """Crea un servidor de prueba simplificado"""
    servidor_content = '''#!/usr/bin/env python3
import http.server
import socketserver
import time

class TestHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        client_ip = self.client_address[0]
        print(f"\\n🎉 CONEXIÓN DESDE: {client_ip}")

        self.send_response(200)
        self.send_header('Content-Type', 'text/html; charset=utf-8')
        self.end_headers()

        html = f"""
        <html><head><meta charset="UTF-8"><title>Éxito</title>
        <style>body{{background:#4CAF50;color:white;text-align:center;padding:50px;font-family:Arial}}</style>
        </head><body>
        <h1>🎉 ¡FUNCIONÓ!</h1>
        <p>Tu celular ({client_ip}) se conectó exitosamente</p>
        <p>Hora: {time.strftime('%H:%M:%S')}</p>
        </body></html>
        """
        self.wfile.write(html.encode())

PORT = 8090
print("🚀 Servidor de prueba iniciado en puerto", PORT)
print("📱 Desde tu celular: http://192.168.1.24:8090")
with socketserver.TCPServer(("0.0.0.0", PORT), TestHandler) as httpd:
    httpd.serve_forever()
'''

    with open("servidor-test-final.py", "w", encoding="utf-8") as f:
        f.write(servidor_content)
    return "servidor-test-final.py"

def main():
    print("🔧 SOLUCIONADOR DE CONECTIVIDAD CELULAR")
    print("=" * 50)

    # 1. Verificar conectividad básica
    print("1️⃣ VERIFICANDO CONECTIVIDAD BÁSICA...")
    gateway_ok, gateway = verificar_ping_gateway()

    if not gateway_ok:
        print("❌ Problema de conectividad básica con el router")
        print("   Verifica que estés conectado a WiFi")
        return

    # 2. Aplicar soluciones automáticas
    intentar_solucion_windows()

    # 3. Crear instrucciones para el router
    print("\n📝 CREANDO INSTRUCCIONES PARA ROUTER...")
    archivo_config = crear_configuracion_router(gateway)

    # 4. Crear servidor de prueba
    print("🖥️  CREANDO SERVIDOR DE PRUEBA...")
    servidor_test = crear_servidor_test()

    print("\n" + "=" * 50)
    print("✅ CONFIGURACIÓN COMPLETADA")
    print("=" * 50)

    print("\n📋 PRÓXIMOS PASOS:")
    print("1️⃣ LEE las instrucciones en:", archivo_config)
    print("2️⃣ CONFIGURA tu router según las instrucciones")
    print("3️⃣ EJECUTA el servidor de prueba:")
    print(f"   python {servidor_test}")
    print("4️⃣ PRUEBA desde tu celular: http://192.168.1.24:8090")

    print("\n🆘 SI AÚN NO FUNCIONA:")
    print("• Prueba conectar tu PC al hotspot de tu celular")
    print("• Verifica que no tengas software antivirus bloqueando")
    print("• Algunos routers requieren reinicio completo")

    # Abrir archivo de configuración
    try:
        os.startfile(archivo_config)
        print(f"\n📖 Abriendo {archivo_config}...")
    except:
        pass

if __name__ == "__main__":
    main()
