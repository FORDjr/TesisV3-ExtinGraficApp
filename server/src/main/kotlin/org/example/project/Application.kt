package org.example.project

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import org.example.project.config.DatabaseConfig
import org.example.project.routes.inventarioRoutes
import org.example.project.routes.authRoutes

fun main() {
    val port = getServerPort()
    println("🚀 Iniciando servidor en puerto $port")
    println("🌍 Entorno: ${if (System.getenv("env") == "production") "PRODUCCIÓN" else "DESARROLLO"}")

    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Inicializar la base de datos
    DatabaseConfig.init()

    // Configurar serialización JSON con configuraciones más flexibles
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    routing {
        get("/") {
            call.respondText("Servidor de Inventario funcionando correctamente")
        }

        // Rutas de autenticación
        authRoutes()

        // Rutas del inventario
        inventarioRoutes()

        // Ruta de estado de la base de datos
        get("/health") {
            call.respondText("Base de datos conectada - Inventario API v1.0")
        }
    }
}