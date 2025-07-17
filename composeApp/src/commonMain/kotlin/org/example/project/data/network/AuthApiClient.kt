package org.example.project.data.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.project.data.models.*
import org.example.project.LOCAL_SERVER_URL

class AuthApiClient {

    // Configuración del cliente HTTP
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }

    // URL base del servidor universitario
    // Usando el servidor desplegado en la universidad
    private val baseUrl = "http://146.83.198.35:1609"

    // Registro de usuario
    suspend fun registrarUsuario(registro: UsuarioRegistro): Result<RegistroResponse> {
        return try {
            val response: RegistroResponse = client.post("$baseUrl/api/auth/registro") {
                contentType(ContentType.Application.Json)
                setBody(registro)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Login de usuario
    suspend fun loginUsuario(login: UsuarioLogin): Result<LoginResponse> {
        return try {
            val response: LoginResponse = client.post("$baseUrl/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(login)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Obtener perfil de usuario
    suspend fun obtenerPerfil(userId: Int): Result<UsuarioResponse> {
        return try {
            val response: UsuarioResponse = client.get("$baseUrl/auth/profile/$userId").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Probar conexión con el servidor
    suspend fun probarConexion(): Result<String> {
        return try {
            val response: String = client.get("$baseUrl/auth/test").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cerrar el cliente
    fun close() {
        client.close()
    }
}
