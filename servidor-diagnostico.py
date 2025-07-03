#!/usr/bin/env python3
"""
Servidor mejorado con diagnóstico automático de conectividad
para solucionar problemas de conexión celular-servidor (sin dependencias externas)
"""
import http.server
import socketserver
import json
import socket
import subprocess
import platform
import threading
import time
import re
from urllib.parse import urlparse

class DiagnosticServer(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        client_ip = self.client_address[0]
        print(f"📱 Conexión recibida desde: {client_ip}")

        # Agregar headers CORS para evitar problemas
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, POST, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')

        if self.path == '/':
            self.send_response(200)
            self.send_header('Content-type', 'text/html; charset=utf-8')
            self.end_headers()

            response = f"""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Servidor Conectado ✅</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <style>
                    body {{ font-family: Arial, sans-serif; margin: 20px; background: #f0f0f0; }}
                    .container {{ background: white; padding: 20px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }}
                    .success {{ color: green; font-size: 24px; margin-bottom: 20px; }}
                    .info {{ background: #e7f3ff; padding: 15px; border-radius: 5px; margin: 10px 0; }}
                    .test-btn {{ background: #4CAF50; color: white; padding: 10px 20px; border: none; border-radius: 5px; margin: 5px; cursor: pointer; }}
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="success">✅ ¡CONEXIÓN EXITOSA!</div>
                    <div class="info">
                        <strong>Tu IP:</strong> {client_ip}<br>
                        <strong>Servidor:</strong> {self.server.server_address[0]}:{self.server.server_address[1]}<br>
                        <strong>Hora:</strong> {time.strftime('%Y-%m-%d %H:%M:%S')}<br>
                        <strong>Tipo de conexión:</strong> {self.detect_connection_type(client_ip)}
                    </div>
                    <p>🎉 Tu celular se conectó correctamente al servidor!</p>
                    <button class="test-btn" onclick="window.location.href='/test'">Probar Endpoint</button>
                    <button class="test-btn" onclick="window.location.href='/health'">Ver Estado</button>
                    <button class="test-btn" onclick="window.location.href='/info'">Info Detallada</button>
                </div>
            </body>
            </html>
            """
            self.wfile.write(response.encode())

        elif self.path == '/test':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {
                "status": "success",
                "message": "✅ Endpoint de prueba funcionando",
                "client_ip": client_ip,
                "server_time": time.strftime('%Y-%m-%d %H:%M:%S'),
                "connection_type": self.detect_connection_type(client_ip)
            }
            self.wfile.write(json.dumps(response, indent=2, ensure_ascii=False).encode('utf-8'))

        elif self.path == '/health':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {
                "status": "healthy",
                "server_info": {
                    "host": self.server.server_address[0],
                    "port": self.server.server_address[1],
                    "platform": platform.system(),
                    "uptime": time.strftime('%Y-%m-%d %H:%M:%S')
                },
                "client_info": {
                    "ip": client_ip,
                    "connection_type": self.detect_connection_type(client_ip)
                }
            }
            self.wfile.write(json.dumps(response, indent=2, ensure_ascii=False).encode('utf-8'))

        elif self.path == '/info':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {
                "network_info": get_network_info(),
                "client_ip": client_ip,
                "server_interfaces": get_server_interfaces(),
                "firewall_status": check_firewall_status()
            }
            self.wfile.write(json.dumps(response, indent=2, ensure_ascii=False).encode('utf-8'))

        else:
            self.send_response(404)
            self.send_header('Content-type', 'text/plain')
            self.end_headers()
            self.wfile.write(b'404 - Endpoint no encontrado')

    def detect_connection_type(self, client_ip):
        """Detecta el tipo de conexión basado en la IP del cliente"""
        if client_ip.startswith('192.168.'):
            return "WiFi Local"
        elif client_ip.startswith('10.0.11.'):
            return "VPN"
        elif client_ip.startswith('10.'):
            return "Red Interna/VPN"
        elif client_ip.startswith('172.'):
            return "Red Privada"
        elif client_ip.startswith('26.'):
            return "Radmin VPN"
        else:
            return "Desconocido"

    def log_message(self, format, *args):
        print(f"🌐 {format % args}")

def get_network_info():
    """Obtiene información de red del sistema usando solo bibliotecas estándar"""
    try:
        interfaces = {}

        # Obtener IP principal
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip_principal = s.getsockname()[0]
        s.close()

        interfaces['Principal'] = {
            'ip': ip_principal,
            'type': detect_interface_type(ip_principal)
        }

        # Obtener IP por hostname
        hostname = socket.gethostname()
        ip_hostname = socket.gethostbyname(hostname)
        if ip_hostname != ip_principal:
            interfaces['Hostname'] = {
                'ip': ip_hostname,
                'type': detect_interface_type(ip_hostname)
            }

        # Intentar obtener más interfaces con ipconfig (Windows)
        if platform.system().lower() == 'windows':
            try:
                result = subprocess.run(['ipconfig'], capture_output=True, text=True)
                output = result.stdout

                current_adapter = None
                for line in output.split('\n'):
                    line = line.strip()
                    if 'adaptador' in line.lower() or 'adapter' in line.lower():
                        current_adapter = line
                    elif 'IPv4' in line and current_adapter:
                        ip_match = re.search(r'(\d+\.\d+\.\d+\.\d+)', line)
                        if ip_match:
                            ip = ip_match.group(1)
                            if ip != '127.0.0.1' and ip not in [info['ip'] for info in interfaces.values()]:
                                interfaces[current_adapter] = {
                                    'ip': ip,
                                    'type': detect_interface_type(ip)
                                }
            except:
                pass

        return interfaces
    except Exception as e:
        return {"error": str(e)}

def detect_interface_type(ip):
    """Detecta el tipo de interfaz basado en la IP"""
    if ip.startswith('192.168.'):
        return "WiFi/LAN"
    elif ip.startswith('10.0.11.'):
        return "VPN"
    elif ip.startswith('10.'):
        return "Red Interna"
    elif ip.startswith('172.'):
        return "Red Privada"
    elif ip.startswith('26.'):
        return "Radmin VPN"
    else:
        return "Desconocido"

def get_server_interfaces():
    """Obtiene las interfaces donde el servidor está escuchando"""
    try:
        hostname = socket.gethostname()
        local_ip = socket.gethostbyname(hostname)
        return {
            "hostname": hostname,
            "local_ip": local_ip,
            "listening_on": "0.0.0.0:8090 (todas las interfaces)"
        }
    except Exception as e:
        return {"error": str(e)}

def check_firewall_status():
    """Verifica el estado del firewall"""
    try:
        cmd = 'netsh advfirewall firewall show rule name="Python 8090" dir=in'
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
        return {
            "firewall_rule_exists": "Python 8090" in result.stdout,
            "recommendation": "Crear regla si no existe" if "Python 8090" not in result.stdout else "Regla configurada correctamente"
        }
    except Exception as e:
        return {"error": str(e)}

def setup_firewall():
    """Configura el firewall automáticamente"""
    try:
        # Eliminar regla existente si existe
        cmd_remove = 'netsh advfirewall firewall delete rule name="Python 8090"'
        subprocess.run(cmd_remove, shell=True, capture_output=True, text=True)

        # Crear nueva regla
        cmd_add = 'netsh advfirewall firewall add rule name="Python 8090" dir=in action=allow protocol=TCP localport=8090'
        result = subprocess.run(cmd_add, shell=True, capture_output=True, text=True)

        if result.returncode == 0:
            print("✅ Regla de firewall creada exitosamente")
            return True
        else:
            print("❌ Error creando regla de firewall - intenta ejecutar como administrador")
            return False
    except Exception as e:
        print(f"❌ Error configurando firewall: {e}")
        return False

def display_connection_info():
    """Muestra información de conexión disponible"""
    print("\n🔍 INFORMACIÓN DE CONECTIVIDAD:")
    print("=" * 50)

    try:
        interfaces = get_network_info()
        urls_disponibles = []

        for interface, info in interfaces.items():
            ip = info['ip']
            tipo = info['type']
            url = f"http://{ip}:8090"
            urls_disponibles.append((tipo, url, interface))
            print(f"📡 {interface}: {ip} ({tipo})")

        print(f"\n📱 URLs para probar en tu celular:")
        for tipo, url, interface in urls_disponibles:
            print(f"   {tipo}: {url}")

        # Recomendaciones específicas
        print(f"\n💡 RECOMENDACIONES:")
        print(f"   🔒 Si usas VPN, asegúrate de que ambos dispositivos estén conectados")
        print(f"   📶 Para WiFi, verifica que estén en la misma red")
        print(f"   🔥 Si no funciona, desactiva temporalmente el firewall")
        print(f"   🔄 Reinicia el servidor después de cambios de red")

    except Exception as e:
        print(f"❌ Error obteniendo información: {e}")

if __name__ == "__main__":
    PORT = 8090

    print("🚀 SERVIDOR DE DIAGNÓSTICO MEJORADO")
    print("=" * 50)

    # Configurar firewall automáticamente
    print("🔧 Configurando firewall...")
    setup_firewall()

    # Mostrar información de conectividad
    display_connection_info()

    print(f"\n🌐 Servidor iniciado en puerto {PORT}")
    print(f"🔥 Presiona Ctrl+C para detener")
    print("=" * 50)

    # Crear servidor
    with socketserver.TCPServer(("0.0.0.0", PORT), DiagnosticServer) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n🛑 Servidor detenido por el usuario")
            print("👋 ¡Hasta luego!")
