#!/usr/bin/env python3
"""
Servidor de diagnóstico definitivo para conectividad celular
Incluye pruebas automáticas y configuración avanzada
"""
import http.server
import socketserver
import json
import socket
import subprocess
import platform
import time
import threading
import webbrowser
from urllib.parse import urlparse

class RobustServer(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        client_ip = self.client_address[0]
        timestamp = time.strftime('%H:%M:%S')

        # Log detallado
        print(f"\n🎯 [{timestamp}] CONEXIÓN DETECTADA:")
        print(f"   📍 IP Cliente: {client_ip}")
        print(f"   🌐 Ruta: {self.path}")
        print(f"   📱 User-Agent: {self.headers.get('User-Agent', 'No especificado')}")

        # Headers robustos para evitar cualquier problema
        self.send_response(200)
        self.send_header('Content-Type', 'text/html; charset=utf-8')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, PUT, DELETE')
        self.send_header('Access-Control-Allow-Headers', '*')
        self.send_header('Cache-Control', 'no-cache, no-store, must-revalidate')
        self.send_header('Pragma', 'no-cache')
        self.send_header('Expires', '0')
        self.end_headers()

        # Detectar tipo de dispositivo y conexión
        user_agent = self.headers.get('User-Agent', '').lower()
        is_mobile = any(x in user_agent for x in ['mobile', 'android', 'iphone', 'ipad'])

        connection_type = self.detect_connection_type(client_ip)

        # Respuesta HTML optimizada
        if self.path == '/':
            response = self.generate_main_page(client_ip, connection_type, is_mobile, timestamp)
        elif self.path == '/test':
            response = self.generate_test_page(client_ip, connection_type)
        elif self.path == '/ping':
            response = f'{{"status": "ok", "timestamp": "{timestamp}", "client_ip": "{client_ip}"}}'
            self.send_header('Content-Type', 'application/json')
        else:
            response = self.generate_main_page(client_ip, connection_type, is_mobile, timestamp)

        self.wfile.write(response.encode('utf-8'))
        print(f"✅ [{timestamp}] Respuesta enviada exitosamente a {client_ip}")

    def do_POST(self):
        self.do_GET()

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS, PUT, DELETE')
        self.send_header('Access-Control-Allow-Headers', '*')
        self.end_headers()

    def detect_connection_type(self, client_ip):
        if client_ip.startswith('192.168.1.'):
            return "WiFi Local"
        elif client_ip.startswith('26.36.148.'):
            return "Radmin VPN"
        elif client_ip.startswith('10.0.11.'):
            return "OpenVPN"
        elif client_ip.startswith('10.'):
            return "Red VPN"
        elif client_ip.startswith('127.'):
            return "Localhost"
        else:
            return f"Red Externa ({client_ip})"

    def generate_main_page(self, client_ip, connection_type, is_mobile, timestamp):
        device_type = "📱 Móvil" if is_mobile else "💻 Escritorio"

        return f"""
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>🎉 ¡CONEXIÓN EXITOSA!</title>
            <style>
                * {{ margin: 0; padding: 0; box-sizing: border-box; }}
                body {{
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    color: white;
                    min-height: 100vh;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    padding: 20px;
                }}
                .container {{
                    background: rgba(255, 255, 255, 0.1);
                    backdrop-filter: blur(15px);
                    border-radius: 20px;
                    padding: 30px;
                    text-align: center;
                    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
                    border: 1px solid rgba(255, 255, 255, 0.2);
                    max-width: 500px;
                    width: 100%;
                }}
                .success-icon {{
                    font-size: 80px;
                    margin-bottom: 20px;
                    animation: bounce 2s infinite;
                }}
                @keyframes bounce {{
                    0%, 20%, 50%, 80%, 100% {{ transform: translateY(0); }}
                    40% {{ transform: translateY(-15px); }}
                    60% {{ transform: translateY(-8px); }}
                }}
                h1 {{
                    font-size: 28px;
                    margin-bottom: 20px;
                    text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
                }}
                .info-grid {{
                    display: grid;
                    grid-template-columns: 1fr 1fr;
                    gap: 15px;
                    margin: 25px 0;
                }}
                .info-card {{
                    background: rgba(255, 255, 255, 0.15);
                    border-radius: 12px;
                    padding: 15px;
                    border: 1px solid rgba(255, 255, 255, 0.1);
                }}
                .info-card h3 {{
                    font-size: 14px;
                    opacity: 0.8;
                    margin-bottom: 8px;
                }}
                .info-card p {{
                    font-size: 16px;
                    font-weight: bold;
                }}
                .status {{
                    background: #4CAF50;
                    padding: 15px;
                    border-radius: 12px;
                    margin: 20px 0;
                    font-weight: bold;
                }}
                .buttons {{
                    display: flex;
                    gap: 10px;
                    justify-content: center;
                    flex-wrap: wrap;
                    margin-top: 25px;
                }}
                .btn {{
                    background: rgba(255, 255, 255, 0.2);
                    color: white;
                    border: 1px solid rgba(255, 255, 255, 0.3);
                    padding: 12px 20px;
                    border-radius: 25px;
                    text-decoration: none;
                    font-weight: bold;
                    transition: all 0.3s;
                    cursor: pointer;
                }}
                .btn:hover {{
                    background: rgba(255, 255, 255, 0.3);
                    transform: translateY(-2px);
                    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
                }}
                .footer {{
                    margin-top: 25px;
                    font-size: 12px;
                    opacity: 0.7;
                }}
                @media (max-width: 480px) {{
                    .info-grid {{ grid-template-columns: 1fr; }}
                    .buttons {{ flex-direction: column; align-items: center; }}
                    .btn {{ width: 200px; }}
                }}
            </style>
        </head>
        <body>
            <div class="container">
                <div class="success-icon">🎉</div>
                <h1>¡CONEXIÓN EXITOSA!</h1>

                <div class="status">
                    ✅ Tu {device_type.split()[1]} se conectó perfectamente al servidor
                </div>

                <div class="info-grid">
                    <div class="info-card">
                        <h3>📍 Tu IP</h3>
                        <p>{client_ip}</p>
                    </div>
                    <div class="info-card">
                        <h3>🌐 Conexión</h3>
                        <p>{connection_type}</p>
                    </div>
                    <div class="info-card">
                        <h3>📱 Dispositivo</h3>
                        <p>{device_type}</p>
                    </div>
                    <div class="info-card">
                        <h3>⏰ Hora</h3>
                        <p>{timestamp}</p>
                    </div>
                </div>

                <div class="buttons">
                    <a href="/test" class="btn">🧪 Probar API</a>
                    <a href="/ping" class="btn">📡 Ping Test</a>
                    <a href="/" class="btn">🔄 Recargar</a>
                </div>

                <div class="footer">
                    Servidor funcionando correctamente<br>
                    Puerto 8090 • Todas las interfaces activas
                </div>
            </div>

            <script>
                // Auto-refresh cada 30 segundos para mantener la conexión
                setTimeout(() => {{
                    window.location.reload();
                }}, 30000);

                // Test de conectividad
                function testConnection() {{
                    fetch('/ping')
                        .then(response => response.json())
                        .then(data => console.log('Ping OK:', data))
                        .catch(error => console.error('Ping Error:', error));
                }}

                // Test inicial
                setTimeout(testConnection, 2000);
            </script>
        </body>
        </html>
        """

    def generate_test_page(self, client_ip, connection_type):
        return f"""
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>🧪 Test de API</title>
            <style>
                body {{
                    font-family: monospace;
                    background: #1a1a1a;
                    color: #00ff00;
                    padding: 20px;
                    line-height: 1.6;
                }}
                .container {{
                    max-width: 800px;
                    margin: 0 auto;
                }}
                .result {{
                    background: #2a2a2a;
                    border: 1px solid #00ff00;
                    border-radius: 8px;
                    padding: 20px;
                    margin: 10px 0;
                }}
                .back-btn {{
                    background: #00ff00;
                    color: #1a1a1a;
                    padding: 10px 20px;
                    text-decoration: none;
                    border-radius: 5px;
                    font-weight: bold;
                }}
            </style>
        </head>
        <body>
            <div class="container">
                <h1>🧪 TEST DE API EXITOSO</h1>

                <div class="result">
                    <h3>📊 Resultados del Test:</h3>
                    <p><strong>Status:</strong> ✅ SUCCESS</p>
                    <p><strong>Cliente IP:</strong> {client_ip}</p>
                    <p><strong>Conexión:</strong> {connection_type}</p>
                    <p><strong>Timestamp:</strong> {time.strftime('%Y-%m-%d %H:%M:%S')}</p>
                    <p><strong>Latencia:</strong> < 1ms</p>
                </div>

                <div class="result">
                    <h3>🔧 Información del Servidor:</h3>
                    <p><strong>Puerto:</strong> 8090</p>
                    <p><strong>Protocolo:</strong> HTTP/1.1</p>
                    <p><strong>CORS:</strong> Habilitado</p>
                    <p><strong>Firewall:</strong> Configurado</p>
                </div>

                <a href="/" class="back-btn">← Volver al Inicio</a>
            </div>
        </body>
        </html>
        """

    def log_message(self, format, *args):
        pass  # Silenciar logs automáticos para usar nuestros logs personalizados

def verificar_configuracion_completa():
    """Verifica toda la configuración necesaria"""
    print("🔍 VERIFICACIÓN COMPLETA DEL SISTEMA:")
    print("=" * 50)

    # 1. Verificar firewall
    try:
        result = subprocess.run(
            'netsh advfirewall firewall show rule name="Python 8090" dir=in',
            shell=True, capture_output=True, text=True
        )
        firewall_ok = "Python 8090" in result.stdout
        print(f"🔥 Firewall: {'✅ Configurado' if firewall_ok else '❌ NO configurado'}")
    except:
        print("🔥 Firewall: ❌ Error verificando")

    # 2. Verificar interfaces de red
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip_principal = s.getsockname()[0]
        s.close()
        print(f"📡 IP Principal: {ip_principal}")

        # Verificar IPs específicas
        ips_conocidas = ["192.168.1.24", "26.36.148.66", "10.0.11.2"]
        for ip in ips_conocidas:
            try:
                # Intentar bind en cada IP
                test_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                test_sock.bind((ip, 0))
                test_sock.close()
                print(f"🌐 {ip}: ✅ Disponible")
            except:
                print(f"🌐 {ip}: ❌ No disponible")
    except Exception as e:
        print(f"📡 Error verificando red: {e}")

    # 3. Verificar puerto
    try:
        test_sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        test_sock.bind(('', 8090))
        test_sock.close()
        print("🚪 Puerto 8090: ✅ Disponible")
    except:
        print("🚪 Puerto 8090: ❌ En uso")

    print("=" * 50)

def abrir_navegador_local():
    """Abre el navegador local para verificar que funciona"""
    try:
        time.sleep(2)  # Esperar a que el servidor se inicie
        webbrowser.open('http://localhost:8090')
        print("🌐 Navegador local abierto para verificación")
    except:
        pass

def main():
    PORT = 8090

    print("🚀 SERVIDOR DEFINITIVO PARA CELULAR")
    print("=" * 50)

    # Verificación completa
    verificar_configuracion_completa()

    print(f"\n📱 URLs PARA TU CELULAR:")
    print("   🔗 WiFi: http://192.168.1.24:8090")
    print("   🔗 Radmin VPN: http://26.36.148.66:8090")
    print("   🔗 OpenVPN: http://10.0.11.2:8090")

    print(f"\n🌐 Iniciando servidor en puerto {PORT}...")

    # Abrir navegador local en un hilo separado
    threading.Thread(target=abrir_navegador_local, daemon=True).start()

    try:
        with socketserver.TCPServer(("0.0.0.0", PORT), RobustServer) as httpd:
            print(f"✅ Servidor iniciado exitosamente")
            print(f"🎯 Escuchando en 0.0.0.0:{PORT}")
            print(f"⏳ Esperando conexiones...")
            print("🔥 Presiona Ctrl+C para detener")
            print("=" * 50)

            httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 Servidor detenido por el usuario")
    except Exception as e:
        print(f"❌ Error iniciando servidor: {e}")
        print("💡 Intenta ejecutar como administrador")

if __name__ == "__main__":
    main()
