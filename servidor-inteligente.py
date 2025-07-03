#!/usr/bin/env python3
"""
Servidor con detección automática de IP para celular
"""
import http.server
import socketserver
import socket
import subprocess
import time
import threading
import webbrowser
import re

class SmartServer(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        client_ip = self.client_address[0]
        timestamp = time.strftime('%H:%M:%S')

        print(f"\n🎉 [{timestamp}] ¡CONEXIÓN EXITOSA desde {client_ip}!")

        self.send_response(200)
        self.send_header('Content-Type', 'text/html; charset=utf-8')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.end_headers()

        # Detectar tipo de conexión
        tipo_conexion = "Desconocido"
        if client_ip.startswith('192.168.1.'):
            tipo_conexion = "WiFi Local"
        elif client_ip.startswith('26.36.148.'):
            tipo_conexion = "Radmin VPN"
        elif client_ip.startswith('10.0.11.'):
            tipo_conexion = "OpenVPN"
        elif client_ip.startswith('127.'):
            tipo_conexion = "Localhost"

        html = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>¡Conexión Exitosa!</title>
            <style>
                body {{
                    font-family: Arial, sans-serif;
                    background: linear-gradient(45deg, #667eea, #764ba2);
                    color: white;
                    margin: 0;
                    padding: 20px;
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }}
                .container {{
                    background: rgba(255,255,255,0.1);
                    padding: 40px;
                    border-radius: 20px;
                    text-align: center;
                    box-shadow: 0 10px 30px rgba(0,0,0,0.3);
                    max-width: 400px;
                }}
                .emoji {{ font-size: 60px; margin-bottom: 20px; }}
                h1 {{ font-size: 24px; margin: 20px 0; }}
                .info {{ background: rgba(255,255,255,0.2); padding: 15px; border-radius: 10px; margin: 20px 0; }}
                .success {{ background: #4CAF50; padding: 15px; border-radius: 10px; margin: 20px 0; }}
                .btn {{
                    background: #4CAF50;
                    color: white;
                    padding: 12px 24px;
                    border: none;
                    border-radius: 25px;
                    text-decoration: none;
                    margin: 10px;
                    display: inline-block;
                }}
            </style>
        </head>
        <body>
            <div class="container">
                <div class="emoji">🎉</div>
                <h1>¡CONEXIÓN EXITOSA!</h1>
                <div class="success">
                    ✅ ¡Problema de conectividad SOLUCIONADO!
                </div>
                <div class="info">
                    <strong>IP de tu celular:</strong> {client_ip}<br>
                    <strong>Tipo de conexión:</strong> {tipo_conexion}<br>
                    <strong>Hora:</strong> {timestamp}
                </div>
                <p>🚀 Tu celular ahora puede comunicarse perfectamente con tu PC!</p>
                <a href="/" class="btn">🔄 Recargar</a>
            </div>
        </body>
        </html>
        """

        self.wfile.write(html.encode('utf-8'))
        print(f"✅ [{timestamp}] Respuesta enviada a {client_ip} ({tipo_conexion})")

def obtener_ip_wifi():
    """Obtiene la IP WiFi actual de la PC"""
    try:
        # Método 1: Conectar a Internet para obtener IP local
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except:
        try:
            # Método 2: Usar ipconfig
            result = subprocess.run(['ipconfig'], capture_output=True, text=True)
            lines = result.stdout.split('\n')
            for i, line in enumerate(lines):
                if 'Wi-Fi' in line or 'Wireless' in line:
                    # Buscar la IP en las siguientes líneas
                    for j in range(i, min(i+10, len(lines))):
                        if 'IPv4' in lines[j]:
                            ip_match = re.search(r'(\d+\.\d+\.\d+\.\d+)', lines[j])
                            if ip_match:
                                return ip_match.group(1)
        except:
            pass
    return None

def verificar_conectividad():
    """Verifica la conectividad y muestra información útil"""
    print("🔍 DIAGNÓSTICO DE CONECTIVIDAD:")
    print("=" * 50)

    # Obtener IP WiFi
    ip_wifi = obtener_ip_wifi()
    if ip_wifi:
        print(f"📡 IP WiFi detectada: {ip_wifi}")
    else:
        print("❌ No se pudo detectar IP WiFi")

    # Verificar firewall
    try:
        result = subprocess.run('netsh advfirewall firewall show rule name="Python 8090" dir=in',
                              shell=True, capture_output=True, text=True)
        if "Python 8090" in result.stdout:
            print("🔥 Firewall: ✅ Configurado")
        else:
            print("🔥 Firewall: ❌ NO configurado")
    except:
        print("🔥 Firewall: ❌ Error verificando")

    print("=" * 50)
    return ip_wifi

def main():
    print("🚀 SERVIDOR INTELIGENTE PARA CELULAR")
    print("=" * 50)

    # Verificar configuración
    ip_wifi = verificar_conectividad()

    print("\n📱 INSTRUCCIONES PARA TU CELULAR:")
    print("=" * 50)

    if ip_wifi:
        url_wifi = f"http://{ip_wifi}:8090"
        print(f"🌐 URL WiFi: {url_wifi}")
        print(f"📱 Abre esta URL en el navegador de tu celular")
    else:
        print("❌ No se pudo detectar la IP WiFi")
        print("📱 Prueba estas URLs en tu celular:")
        print("   • http://192.168.1.24:8090")
        print("   • http://10.0.11.2:8090")
        print("   • http://26.36.148.66:8090")

    print("\n💡 NOTA IMPORTANTE:")
    print("   Si tu celular está en 192.168.1.4")
    print("   y tu PC en una IP diferente,")
    print("   es posible que haya aislamiento AP.")
    print("   Intenta desactivar 'AP Isolation' en tu router.")

    print("\n🌐 Iniciando servidor en puerto 8090...")
    print("=" * 50)

    try:
        with socketserver.TCPServer(("0.0.0.0", 8090), SmartServer) as httpd:
            print("✅ Servidor iniciado exitosamente")
            print("⏳ Esperando conexiones desde tu celular...")
            print("🔥 Presiona Ctrl+C para detener")
            print("=" * 50)
            httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 Servidor detenido")
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    main()
