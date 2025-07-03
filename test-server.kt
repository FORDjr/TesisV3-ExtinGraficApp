package org.example.project

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    println("🚀 Iniciando servidor básico de prueba...")
    println("📱 Tu celular debería conectarse a:")
    println("   - Wi-Fi: http://192.168.1.24:8090")
    println("   - VPN: http://10.0.11.2:8090")

    embeddedServer(Netty, port = 8090, host = "0.0.0.0") {
        routing {
            get("/") {
                println("📱 Conexión recibida desde celular!")
                call.respondText("✅ ¡Hola desde tu PC! Conexión exitosa")
            }

            get("/test") {
                println("🔗 Prueba de conectividad exitosa!")
                call.respondText("📱 Tu celular se conectó correctamente")
            }

            get("/health") {
                call.respondText("Servidor funcionando en puerto 8090")
            }
        }
    }.start(wait = true)
}
