#!/usr/bin/env python3
"""
Servidor de prueba específico para conectividad celular
Con configuración automática y diagnóstico completo
"""
import http.server
import socketserver
import json
import socket
import subprocess
import platform
import time
import threading
from urllib.parse import urlparse

class TestServer(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        client_ip = self.client_address[0]
        timestamp = time.strftime('%H:%M:%S')

        print(f"🎉 [{timestamp}] CONEXIÓN EXITOSA desde {client_ip}")

        # Headers para evitar problemas CORS
        self.send_response(200)
        self.send_header('Content-type', 'text/html; charset=utf-8')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()

        # Detectar tipo de conexión
        connection_type = "Desconocido"
        if client_ip.startswith('192.168.1.'):
            connection_type = "WiFi Local"
        elif client_ip.startswith('26.36.148.'):
            connection_type = "Radmin VPN"
        elif client_ip.startswith('10.0.11.'):
            connection_type = "OpenVPN"
        elif client_ip.startswith('10.'):
            connection_type = "Red VPN"

        response = f"""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>🎉 ¡CONEXIÓN EXITOSA!</title>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                body {{
                    font-family: Arial, sans-serif;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    margin: 0;
                    padding: 20px;
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }}
                .container {{
                    background: rgba(255, 255, 255, 0.1);
                    backdrop-filter: blur(10px);
                    border-radius: 20px;
                    padding: 30px;
                    text-align: center;
                    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
                    max-width: 400px;
                    width: 100%;
                }}
                .success {{
                    font-size: 48px;
                    margin-bottom: 20px;
                    animation: bounce 2s infinite;
                }}
                @keyframes bounce {{
                    0%, 20%, 50%, 80%, 100% {{ transform: translateY(0); }}
                    40% {{ transform: translateY(-10px); }}
                    60% {{ transform: translateY(-5px); }}
                }}
                .info {{
                    background: rgba(255, 255, 255, 0.2);
                    border-radius: 10px;
                    padding: 20px;
                    margin: 20px 0;
                }}
                .info h3 {{
                    margin-top: 0;
                    color: #ffeb3b;
                }}
                .button {{
                    background: #4CAF50;
                    color: white;
                    border: none;
                    padding: 15px 30px;
                    border-radius: 25px;
                    font-size: 16px;
                    cursor: pointer;
                    margin: 10px;
                    transition: all 0.3s;
                    text-decoration: none;
                    display: inline-block;
                }}
                .button:hover {{
                    background: #45a049;
                    transform: translateY(-2px);
                    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
                }}
                .stats {{
                    font-size: 14px;
                    opacity: 0.8;
                    margin-top: 20px;
                }}
            </style>
        </head>
        <body>
            <div class="container">
                <div class="success">🎉</div>
                <h1>¡CONEXIÓN EXITOSA!</h1>
                <div class="info">
                    <h3>📱 Información de tu conexión:</h3>
                    <p><strong>Tu IP:</strong> {client_ip}</p>
                    <p><strong>Tipo:</strong> {connection_type}</p>
                    <p><strong>Servidor:</strong> {self.server.server_address[1]}</p>
                    <p><strong>Hora:</strong> {timestamp}</p>
                </div>
                <p>✅ Tu celular se conectó perfectamente al servidor de tu PC!</p>
                <a href="/test" class="button">🧪 Probar API</a>
                <a href="/info" class="button">📊 Ver Detalles</a>
                <div class="stats">
                    Conexión establecida correctamente<br>
                    Servidor funcionando en todas las interfaces
                </div>
            </div>
        </body>
        </html>
        """

        self.wfile.write(response.encode('utf-8'))

        # Log en consola
        print(f"✅ [{timestamp}] Respuesta enviada a {client_ip} ({connection_type})")

    def do_POST(self):
        # Manejar requests POST
        self.do_GET()

    def do_OPTIONS(self):
        # Manejar preflight requests
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()

    def log_message(self, format, *args):
        # Log personalizado
        timestamp = time.strftime('%H:%M:%S')
        print(f"🌐 [{timestamp}] {format % args}")

def verificar_firewall():
    """Verifica si el firewall está configurado correctamente"""
    try:
        result = subprocess.run(
            'netsh advfirewall firewall show rule name="Python 8090" dir=in',
            shell=True, capture_output=True, text=True
        )
        return "Python 8090" in result.stdout
    except:
        return False

def obtener_ips_locales():
    """Obtiene todas las IPs locales disponibles"""
    ips = []
    try:
        # IP principal
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip_principal = s.getsockname()[0]
        s.close()
        ips.append(ip_principal)

        # IP por hostname
        hostname = socket.gethostname()
        ip_hostname = socket.gethostbyname(hostname)
        if ip_hostname not in ips:
            ips.append(ip_hostname)

    except Exception as e:
        print(f"❌ Error obteniendo IPs: {e}")

    return ips

def mostrar_urls_para_celular():
    """Muestra las URLs que el celular puede usar"""
    print("\n📱 URLs PARA TU CELULAR:")
    print("=" * 40)

    # URLs específicas conocidas
    urls = [
        ("WiFi Local", "http://192.168.1.24:8090"),
        ("Radmin VPN", "http://26.36.148.66:8090"),
        ("OpenVPN", "http://10.0.11.2:8090")
    ]

    for tipo, url in urls:
        print(f"   🔗 {tipo}: {url}")

    print("\n💡 INSTRUCCIONES:")
    print("   1. Abre el navegador en tu celular")
    print("   2. Escribe una de las URLs de arriba")
    print("   3. Deberías ver una página de éxito")
    print("   4. Si no funciona, verifica que:")
    print("      - Tu celular esté en la misma red WiFi")
    print("      - O conectado a la misma VPN")
    print("      - El firewall esté configurado")

def verificar_puerto_disponible(puerto):
    """Verifica si un puerto está disponible"""
    try:
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            s.bind(('', puerto))
            return True
    except:
        return False

def main():
    PORT = 8090

    print("🚀 SERVIDOR DE PRUEBA PARA CELULAR")
    print("=" * 50)

    # Verificar puerto
    if not verificar_puerto_disponible(PORT):
        print(f"❌ Puerto {PORT} ya está en uso")
        print("   Cerrando procesos que usen el puerto...")
        try:
            subprocess.run(f'taskkill /F /FI "WINDOWTITLE eq *8090*"', shell=True)
            time.sleep(2)
        except:
            pass

    # Verificar firewall
    if verificar_firewall():
        print("✅ Firewall configurado correctamente")
    else:
        print("❌ Firewall NO configurado")
        print("   👉 Ejecuta 'configurar-firewall.bat' como administrador")

    # Mostrar URLs
    mostrar_urls_para_celular()

    print(f"\n🌐 Iniciando servidor en puerto {PORT}...")
    print("🔥 Presiona Ctrl+C para detener")
    print("=" * 50)

    try:
        with socketserver.TCPServer(("0.0.0.0", PORT), TestServer) as httpd:
            print(f"✅ Servidor iniciado en 0.0.0.0:{PORT}")
            print("⏳ Esperando conexiones desde tu celular...")
            httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 Servidor detenido")
    except Exception as e:
        print(f"❌ Error: {e}")
        print("   💡 Intenta ejecutar como administrador")

if __name__ == "__main__":
    main()
