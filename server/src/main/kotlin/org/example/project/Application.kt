package org.example.project

import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import org.example.project.config.DatabaseConfig
import org.example.project.routes.authRoutes
import org.example.project.routes.inventarioRoutes
import org.example.project.routes.ventasRoutes
import org.example.project.routes.ventasPublicRoutes
import io.ktor.server.plugins.cors.routing.*
import io.ktor.http.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.request.path // para path()
import org.slf4j.event.Level
import java.util.UUID
import org.jetbrains.exposed.sql.transactions.transaction
import org.example.project.routes.extintoresRoutes
import org.example.project.routes.dashboardRoutes
import org.example.project.routes.movimientosRoutes
import org.example.project.routes.movimientosPublicRoutes
import org.example.project.services.AlertScheduler
import org.example.project.services.ExtintoresService
import org.example.project.routes.integracionesRoutes
import org.example.project.JWT_REALM
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import org.example.project.security.JwtConfig
import org.example.project.routes.usuariosRoutes

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

    if (!DISABLE_SCHEDULER) {
        // Scheduler alertas (intervalo 60m en prod, 10m en dev)
        val interval = if (IS_PRODUCTION) 60L else 10L
        AlertScheduler(ExtintoresService()).start(interval)
    } else {
        println("[Scheduler] Deshabilitado por DISABLE_SCHEDULER=true")
    }

    /* ---------- Serialización JSON ---------- */
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint       = true
                isLenient         = true
                ignoreUnknownKeys = true
                encodeDefaults    = true
            }
        )
    }

    /* ---------- Auth JWT ---------- */
    install(Authentication) {
        jwt("auth-jwt") {
            realm = JWT_REALM
            verifier(JwtConfig.verifier())
            validate { credential ->
                val role = credential.payload.getClaim("role").asString()
                val userId = credential.payload.getClaim("userId").asInt()
                if (role.isNullOrBlank() || userId == null) null else JWTPrincipal(credential.payload)
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Token inválido o expirado"))
            }
        }
    }

    /* ---------- Observabilidad / Logging ---------- */
    install(CallId) {
        header("X-Request-Id")
        generate { UUID.randomUUID().toString() }
        verify { it.isNotBlank() }
    }

    /* ---------- CORS ---------- */
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("X-Request-Id")
        allowCredentials = false
        if (!IS_PRODUCTION && ALLOWED_ORIGINS == "*") {
            anyHost()
        } else {
            ALLOWED_ORIGINS.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { origin ->
                    val clean = origin.removePrefix("https://").removePrefix("http://")
                    allowHost(clean, schemes = listOf("http", "https"))
                }
        }
    }

    /* ---------- Rutas ---------- */
    routing {
        get("/") {
            call.respondText("Servidor de Inventario y Ventas funcionando correctamente")
        }

        // Rutas de autenticación
        authRoutes()

        // Rutas de integraciones externas con API key
        integracionesRoutes()

        // Export/descargas con token en query (sin exigir header)
        ventasPublicRoutes()
        movimientosPublicRoutes()

        authenticate("auth-jwt") {
            // Rutas de inventario
            inventarioRoutes()
            movimientosRoutes()

            // Rutas de ventas
            ventasRoutes()

            // Rutas de extintores
            extintoresRoutes()

            // Rutas de dashboard
            dashboardRoutes()

            // Rutas de usuarios
            usuariosRoutes()
        }

        // Health con GET y HEAD
        route("/health") {
            get { call.respondText("OK") }
            head { call.respondText("") }
            get("/db") {
                val (statusText, code) = try {
                    transaction { 1 }
                    "OK" to HttpStatusCode.OK
                } catch (e: Exception) {
                    ("FAIL:${e.message?.take(120)}") to HttpStatusCode.InternalServerError
                }
                call.respondText(statusText, status = code)
            }
            get("/info") {
                val dbOk = try {
                    transaction { 1 }; true
                } catch (_: Exception) { false }
                val payload = HealthInfo(
                    status = "OK",
                    version = API_VERSION,
                    environment = if (IS_PRODUCTION) "production" else "development",
                    db = if (dbOk) "UP" else "DOWN",
                    timestamp = System.currentTimeMillis()
                )
                call.respondText(
                    text = Json.encodeToString(HealthInfo.serializer(), payload),
                    contentType = ContentType.Application.Json
                )
            }
        }

        get("/version") {
            call.respondText("Inventario y Ventas API v1.0")
        }
    }
}

@Serializable
private data class HealthInfo(
    val status: String,
    val version: String,
    val environment: String,
    val db: String,
    val timestamp: Long
)
