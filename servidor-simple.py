#!/usr/bin/env python3
import http.server
import socketserver
import socket
import subprocess
import time
import threading
import webbrowser

class CelularServer(http.server.BaseHTTPRequestHandler):
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
                <div class="info">
                    <strong>Tu IP:</strong> {client_ip}<br>
                    <strong>Conexión:</strong> {tipo_conexion}<br>
                    <strong>Hora:</strong> {timestamp}
                </div>
                <p>✅ Tu celular se conectó perfectamente al servidor de tu PC!</p>
                <a href="/" class="btn">🔄 Recargar</a>
            </div>
        </body>
        </html>
        """

        self.wfile.write(html.encode('utf-8'))
        print(f"✅ [{timestamp}] Respuesta enviada a {client_ip} ({tipo_conexion})")

def verificar_sistema():
    print("🔍 VERIFICACIÓN DEL SISTEMA:")
    print("=" * 40)

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

    print("=" * 40)

def abrir_navegador():
    time.sleep(2)
    try:
        webbrowser.open('http://localhost:8090')
    except:
        pass

def main():
    print("🚀 SERVIDOR SIMPLE PARA CELULAR")
    print("=" * 40)

    verificar_sistema()

    print("\n📱 URLs PARA TU CELULAR:")
    print("   WiFi: http://192.168.1.24:8090")
    print("   Radmin VPN: http://26.36.148.66:8090")
    print("   OpenVPN: http://10.0.11.2:8090")

    print("\n🌐 Iniciando servidor en puerto 8090...")

    # Abrir navegador para verificar
    threading.Thread(target=abrir_navegador, daemon=True).start()

    try:
        with socketserver.TCPServer(("0.0.0.0", 8090), CelularServer) as httpd:
            print("✅ Servidor iniciado exitosamente")
            print("⏳ Esperando conexiones...")
            print("🔥 Presiona Ctrl+C para detener")
            print("=" * 40)
            httpd.serve_forever()
    except KeyboardInterrupt:
        print("\n🛑 Servidor detenido")
    except Exception as e:
        print(f"❌ Error: {e}")

if __name__ == "__main__":
    main()
