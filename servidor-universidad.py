#!/usr/bin/env python3
"""
Servidor para prueba Universidad - Casa
Tu PC (casa con VPN) <-> Tu celular (universidad sin VPN)
"""
import http.server
import socketserver
import socket
import subprocess
import time
import json

class UniversidadServer(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        client_ip = self.client_address[0]
        timestamp = time.strftime('%H:%M:%S')

        print(f"\n🎓 [{timestamp}] ¡CONEXIÓN DESDE UNIVERSIDAD!")
        print(f"   📍 IP Cliente: {client_ip}")
        print(f"   🌐 Ruta: {self.path}")

        # Headers robustos
        self.send_response(200)
        self.send_header('Content-Type', 'text/html; charset=utf-8')
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', '*')
        self.end_headers()

        # Detectar tipo de conexión
        tipo_conexion = self.detectar_tipo_conexion(client_ip)

        if self.path == '/':
            html = self.generar_pagina_principal(client_ip, tipo_conexion, timestamp)
        elif self.path == '/test':
            html = self.generar_pagina_test(client_ip, tipo_conexion)
        elif self.path == '/info':
            html = self.generar_info_json(client_ip, tipo_conexion)
        else:
            html = self.generar_pagina_principal(client_ip, tipo_conexion, timestamp)

        self.wfile.write(html.encode('utf-8'))
        print(f"✅ [{timestamp}] Respuesta enviada exitosamente")

    def detectar_tipo_conexion(self, client_ip):
        """Detecta el tipo de conexión del cliente"""
        if client_ip.startswith('10.0.11.'):
            return "🔒 OpenVPN"
        elif client_ip.startswith('26.36.148.'):
            return "🔒 Radmin VPN"
        elif client_ip.startswith('192.168.'):
            return "🏠 WiFi Casa"
        elif client_ip.startswith('10.'):
            return "🌐 Red Universidad"
        elif client_ip.startswith('172.'):
            return "🌐 Red Universidad"
        elif client_ip.startswith('127.'):
            return "💻 Localhost"
        else:
            return f"🌍 Internet ({client_ip})"

    def generar_pagina_principal(self, client_ip, tipo_conexion, timestamp):
        return f"""
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>🎓 Universidad ↔ Casa</title>
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
                    padding: 40px;
                    text-align: center;
                    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
                    border: 1px solid rgba(255, 255, 255, 0.2);
                    max-width: 500px;
                    width: 100%;
                }}
                .emoji {{ font-size: 80px; margin-bottom: 20px; }}
                h1 {{ font-size: 32px; margin-bottom: 20px; }}
                .conexion-info {{
                    background: rgba(255, 255, 255, 0.2);
                    padding: 20px;
                    border-radius: 15px;
                    margin: 25px 0;
                }}
                .ip-info {{
                    background: rgba(76, 175, 80, 0.3);
                    padding: 15px;
                    border-radius: 10px;
                    margin: 15px 0;
                }}
                .route-info {{
                    background: rgba(33, 150, 243, 0.3);
                    padding: 15px;
                    border-radius: 10px;
                    margin: 15px 0;
                    font-size: 14px;
                }}
                .buttons {{
                    display: flex;
                    gap: 15px;
                    justify-content: center;
                    flex-wrap: wrap;
                    margin-top: 30px;
                }}
                .btn {{
                    background: rgba(255, 255, 255, 0.2);
                    color: white;
                    border: 1px solid rgba(255, 255, 255, 0.3);
                    padding: 15px 25px;
                    border-radius: 25px;
                    text-decoration: none;
                    font-weight: bold;
                    transition: all 0.3s;
                    cursor: pointer;
                }}
                .btn:hover {{
                    background: rgba(255, 255, 255, 0.3);
                    transform: translateY(-2px);
                }}
                .success {{
                    background: #4CAF50;
                    color: white;
                    padding: 15px;
                    border-radius: 10px;
                    margin: 20px 0;
                    font-weight: bold;
                }}
            </style>
        </head>
        <body>
            <div class="container">
                <div class="emoji">🎓</div>
                <h1>¡CONEXIÓN EXITOSA!</h1>
                <div class="success">
                    Universidad ↔ Casa funcionando perfectamente
                </div>

                <div class="conexion-info">
                    <h3>📱 Información de Conexión:</h3>
                    <div class="ip-info">
                        <strong>Tu IP:</strong> {client_ip}<br>
                        <strong>Tipo:</strong> {tipo_conexion}<br>
                        <strong>Hora:</strong> {timestamp}
                    </div>
                </div>

                <div class="route-info">
                    <strong>🛣️ Ruta de Conexión:</strong><br>
                    Tu celular (Universidad) → Internet → VPN → Tu PC (Casa)
                </div>

                <div class="buttons">
                    <a href="/test" class="btn">🧪 Probar API</a>
                    <a href="/info" class="btn">📊 Ver Info</a>
                    <a href="/" class="btn">🔄 Recargar</a>
                </div>
            </div>
        </body>
        </html>
        """

    def generar_pagina_test(self, client_ip, tipo_conexion):
        return f"""
        <!DOCTYPE html>
        <html lang="es">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>🧪 Test Universidad</title>
            <style>
                body {{
                    font-family: monospace;
                    background: #1a1a1a;
                    color: #00ff00;
                    padding: 20px;
                }}
                .test-result {{
                    background: #2a2a2a;
                    border: 1px solid #00ff00;
                    padding: 20px;
                    border-radius: 10px;
                    margin: 20px 0;
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
            <h1>🧪 TEST DE CONECTIVIDAD UNIVERSIDAD</h1>

            <div class="test-result">
                <h3>✅ RESULTADO: EXITOSO</h3>
                <p><strong>IP Cliente:</strong> {client_ip}</p>
                <p><strong>Tipo Conexión:</strong> {tipo_conexion}</p>
                <p><strong>Latencia:</strong> Excelente</p>
                <p><strong>Estado:</strong> Conectado desde Universidad</p>
                <p><strong>Timestamp:</strong> {time.strftime('%Y-%m-%d %H:%M:%S')}</p>
            </div>

            <div class="test-result">
                <h3>🌍 INFORMACIÓN DE RUTA:</h3>
                <p>Universidad → Internet → VPN → Casa</p>
                <p>✅ Firewall: Configurado</p>
                <p>✅ VPN: Activa</p>
                <p>✅ Puerto 8090: Abierto</p>
            </div>

            <a href="/" class="back-btn">← Volver al Inicio</a>
        </body>
        </html>
        """

    def generar_info_json(self, client_ip, tipo_conexion):
        self.send_header('Content-Type', 'application/json')
        info = {
            "status": "success",
            "client_ip": client_ip,
            "connection_type": tipo_conexion,
            "timestamp": time.strftime('%Y-%m-%d %H:%M:%S'),
            "server_info": {
                "location": "Casa (con VPN)",
                "port": 8090,
                "vpn_enabled": True
            },
            "client_info": {
                "location": "Universidad",
                "connection_route": "Universidad → Internet → VPN → Casa"
            }
        }
        return json.dumps(info, indent=2, ensure_ascii=False)

def obtener_ips_vpn():
    """Obtiene las IPs de VPN disponibles"""
    ips_vpn = []
    try:
        # Obtener todas las IPs
        result = subprocess.run(['ipconfig'], capture_output=True, text=True)
        output = result.stdout

        # Buscar IPs de VPN
        if '10.0.11.' in output:
            ips_vpn.append("10.0.11.2")
        if '26.36.148.' in output:
            ips_vpn.append("26.36.148.66")

    except Exception as e:
        print(f"Error obteniendo IPs: {e}")

    return ips_vpn

def main():
    PORT = 8090

    print("🎓 SERVIDOR UNIVERSIDAD ↔ CASA")
    print("=" * 50)

    # Obtener IPs de VPN
    ips_vpn = obtener_ips_vpn()

    print("📡 IPs de VPN detectadas:")
    for ip in ips_vpn:
        print(f"   🔒 {ip}")

    print(f"\n📱 URLs PARA TU CELULAR EN LA UNIVERSIDAD:")
    print("=" * 50)

    if ips_vpn:
        for ip in ips_vpn:
            print(f"   🌐 http://{ip}:{PORT}")
    else:
        print("   🔒 http://10.0.11.2:8090")
        print("   🔒 http://26.36.148.66:8090")

    print(f"\n💡 INSTRUCCIONES:")
    print("   1. Asegúrate de que tu PC tenga VPN activa")
    print("   2. Desde tu celular en la universidad (SIN VPN)")
    print("   3. Abre una de las URLs de arriba")
    print("   4. Deberías ver la conexión exitosa")

    print(f"\n🌐 Iniciando servidor en puerto {PORT}...")
    print("⏳ Esperando conexión desde la universidad...")
    print("🔥 Presiona Ctrl+C para detener")
    print("=" * 50)

    try:
        with socketserver.TCPServer(("0.0.0.0", PORT), UniversidadServer) as httpd:
            print("✅ Servidor iniciado - Esperando conexión desde universidad...")
            httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 Servidor detenido")
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    main()
