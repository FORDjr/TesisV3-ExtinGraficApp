$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://+:8090/")
$listener.Start()

Write-Host "🚀 Servidor HTTP iniciado en puerto 8090"
Write-Host "📱 Tu celular puede conectarse a:"
Write-Host "   - Wi-Fi: http://192.168.1.24:8090"
Write-Host "   - VPN: http://10.0.11.2:8090"
Write-Host "   - Radmin: http://26.36.148.66:8090"
Write-Host "🔥 Presiona Ctrl+C para detener"

try {
    while ($listener.IsListening) {netsh advfirewall firewall add rule name="Ktor Server 8081" dir=in action=allow protocol=TCP localport=8081
        $context = $listener.GetContext()
        $request = $context.Request
        $response = $context.Response

        Write-Host "📱 Conexión recibida desde: $($request.RemoteEndPoint.Address)"

        $responseString = "✅ ¡Hola desde tu PC! Conexión exitosa entre servidor y celular"
        $buffer = [System.Text.Encoding]::UTF8.GetBytes($responseString)

        $response.ContentLength64 = $buffer.Length
        $response.ContentType = "text/plain; charset=utf-8"
        $response.Headers.Add("Access-Control-Allow-Origin", "*")
        $response.OutputStream.Write($buffer, 0, $buffer.Length)
        $response.OutputStream.Close()

        Write-Host "✅ Respuesta enviada al celular"
    }
}
finally {
    $listener.Stop()
    Write-Host "🛑 Servidor detenido"
}
