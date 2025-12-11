package org.example.project.data.api

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.example.project.data.models.ActualizarUsuarioRequest
import org.example.project.data.models.CambiarEstadoUsuarioRequest
import org.example.project.data.models.CrearUsuarioRequest
import org.example.project.data.models.UsuarioResponse
import org.example.project.data.auth.AuthManager
import org.example.project.LOCAL_SERVER_URL
import org.example.project.preferredBaseUrl

class UsuariosApiService {
    private val http = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("UsuariosApiService: $message")
                }
            }
            level = LogLevel.INFO
        }
        defaultRequest {
            val token = AuthManager.getToken()
            if (token.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    private val bases = listOf(preferredBaseUrl(), LOCAL_SERVER_URL, "http://10.0.2.2:8080")

    private suspend fun <T> first(block: suspend (String) -> T): T {
        val errors = mutableListOf<String>()
        for (base in bases) {
            try {
                return block(base)
            } catch (e: Exception) {
                errors += "$base: ${e.message}"
            }
        }
        throw IllegalStateException("Servidor no disponible: ${errors.joinToString(" | ")}")
    }

    suspend fun listar(): List<UsuarioResponse> = first { base ->
        http.get("$base/api/usuarios").body()
    }

    suspend fun crear(req: CrearUsuarioRequest): UsuarioResponse = first { base ->
        val resp = http.post("$base/api/usuarios") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(req)
        }
        if (!resp.status.isSuccess()) throw IllegalStateException("HTTP ${resp.status}")
        resp.body()
    }

    suspend fun actualizar(id: Int, req: ActualizarUsuarioRequest): UsuarioResponse = first { base ->
        val resp = http.put("$base/api/usuarios/$id") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(req)
        }
        if (!resp.status.isSuccess()) throw IllegalStateException("HTTP ${resp.status}")
        resp.body()
    }

    suspend fun cambiarEstado(id: Int, activo: Boolean): UsuarioResponse = first { base ->
        val resp = http.patch("$base/api/usuarios/$id/estado") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(CambiarEstadoUsuarioRequest(activo))
        }
        if (!resp.status.isSuccess()) throw IllegalStateException("HTTP ${resp.status}")
        resp.body()
    }

    suspend fun obtenerActual(): UsuarioResponse = first { base ->
        http.get("$base/api/usuarios/me").body()
    }

    suspend fun actualizarActual(req: ActualizarUsuarioRequest): UsuarioResponse = first { base ->
        val resp = http.put("$base/api/usuarios/me") {
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(req)
        }
        if (!resp.status.isSuccess()) throw IllegalStateException("HTTP ${resp.status}")
        resp.body()
    }
}
