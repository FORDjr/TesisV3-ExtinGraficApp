#!/usr/bin/env python3
import http.server
import socketserver
import time

class TestHandler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        client_ip = self.client_address[0]
        print(f"\n🎉 CONEXIÓN DESDE: {client_ip}")

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
