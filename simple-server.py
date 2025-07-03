#!/usr/bin/env python3
import http.server
import socketserver
import json
from urllib.parse import urlparse

class SimpleHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        print(f"📱 Petición recibida desde: {self.client_address[0]}")

        if self.path == '/':
            self.send_response(200)
            self.send_header('Content-type', 'text/plain')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(b'Hola desde tu PC! Conexion exitosa')

        elif self.path == '/test':
            self.send_response(200)
            self.send_header('Content-type', 'text/plain')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(b'Tu celular se conecto correctamente al servidor')

        elif self.path == '/health':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            response = {"status": "ok", "message": "Servidor funcionando - Puerto 8090"}
            self.wfile.write(json.dumps(response).encode())
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        print(f"🌐 {format % args}")

if __name__ == "__main__":
    PORT = 8090
    print(f"🚀 Iniciando servidor simple en puerto {PORT}")
    print(f"📱 URLs para tu celular:")
    print(f"   - Wi-Fi: http://192.168.1.24:8090")
    print(f"   - VPN: http://10.0.11.2:8090")
    print(f"💻 Prueba local: http://localhost:8090")
    print(f"🔥 Presiona Ctrl+C para detener")

    with socketserver.TCPServer(("0.0.0.0", PORT), SimpleHandler) as httpd:
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print(f"\n🛑 Servidor detenido")
