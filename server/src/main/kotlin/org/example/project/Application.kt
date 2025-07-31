package org.example.project

import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.project.config.DatabaseConfig
import org.example.project.routes.authRoutes
import org.example.project.routes.inventarioRoutes
import org.example.project.routes.ventasRoutes

fun main() {
    println("🚀 Iniciando servidor en puerto $SERVER_PORT")
    println("🌍 Entorno: ${if (System.getenv("env") == "production") "PRODUCCIÓN" else "DESARROLLO"}")

    embeddedServer(
        Netty,
        host = "0.0.0.0",
        port = SERVER_PORT,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    /* ---------- Base de datos ---------- */
    DatabaseConfig.init()

    /* ---------- Serialización JSON ---------- */
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint       = true
                isLenient         = true
                ignoreUnknownKeys = true
            }
        )
    }

    /* ---------- Rutas ---------- */
    routing {
        get("/") {
            call.respondText("Servidor de Inventario y Ventas funcionando correctamente")
        }

        // Rutas de autenticación
        authRoutes()

        // Rutas de inventario
        inventarioRoutes()

        // Rutas de ventas
        ventasRoutes()

        // Endpoint de salud
        get("/health") {
            call.respondText("Base de datos conectada • Inventario y Ventas API v1.0")
        }
    }
}
