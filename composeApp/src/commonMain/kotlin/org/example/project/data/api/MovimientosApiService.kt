package org.example.project.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.project.LOCAL_SERVER_URL
import org.example.project.data.model.*
import org.example.project.preferredBaseUrl
import io.ktor.client.statement.bodyAsText
import org.example.project.data.auth.AuthManager
import kotlinx.datetime.Clock

class MovimientosApiService {
    companion object {
        private const val API_PATH = "/api/movimientos"
        private val CONNECTION_URLS = listOf(
            preferredBaseUrl(),
            LOCAL_SERVER_URL,
            "http://10.0.2.2:8080"
        )
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000L
            connectTimeoutMillis = 8000L
            socketTimeoutMillis = 15000L
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        install(io.ktor.client.plugins.DefaultRequest) {
            val token = AuthManager.getToken()
            if (token.isNotBlank()) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    private var workingUrl: String? = null

    private suspend fun findWorkingUrl(): String? {
        workingUrl?.let { cached ->
            if (testConnectionQuick(cached)) return cached
            workingUrl = null
        }
        for (url in CONNECTION_URLS) {
            if (url.isBlank()) continue
            if (testConnection(url)) {
                workingUrl = url
                return url
            }
        }
        return null
    }

    private suspend fun testConnectionQuick(url: String): Boolean = try {
        val resp = httpClient.get("$url$API_PATH") {
            parameter("limit", 1)
        }
        resp.status.isSuccess()
    } catch (_: Exception) {
        false
    }

    private suspend fun testConnection(url: String): Boolean {
        return try {
            val resp = httpClient.get("$url/health")
            if (resp.status.isSuccess()) {
                true
            } else {
                testConnectionQuick(url)
            }
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun <T> makeRequest(block: suspend (String) -> T): T {
        val base = findWorkingUrl() ?: throw Exception("Servidor no disponible")
        return try {
            block(base)
        } catch (_: Exception) {
            workingUrl = null
            val fallback = findWorkingUrl() ?: throw Exception("Sin conexión al servidor")
            block(fallback)
        }
    }

    suspend fun listarMovimientos(
        filtros: KardexFilters,
        limit: Int,
        offset: Int
    ): MovimientosPage = makeRequest { base ->
        httpClient.get("$base$API_PATH") {
            parameter("limit", limit)
            parameter("offset", offset)
            applyFilters(filtros)
        }.body()
    }

    suspend fun obtenerKardex(filtros: KardexFilters): KardexResponse = makeRequest { base ->
        val resp = httpClient.get("$base$API_PATH/kardex") {
            applyFilters(filtros)
        }
        if (!resp.status.isSuccess()) {
            val msg = resp.bodyAsText()
            throw IllegalStateException(msg.ifBlank { "Error al obtener kardex (${resp.status})" })
        }
        resp.body()
    }

    suspend fun crearMovimiento(request: CrearMovimientoRequest): MovimientoInventario = makeRequest { base ->
        val payload = if (request.idempotenciaKey.isNullOrBlank()) {
            request.copy(idempotenciaKey = "mov-${Clock.System.now().toEpochMilliseconds()}")
        } else request
        httpClient.post("$base$API_PATH") {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }.body()
    }

    suspend fun aprobarMovimiento(
        id: Int,
        aprobado: Boolean,
        observaciones: String? = null,
        usuarioId: Int? = null
    ): MovimientoInventario = makeRequest { base ->
        httpClient.post("$base$API_PATH/$id/aprobar") {
            contentType(ContentType.Application.Json)
            setBody(
                AprobarMovimientoPayload(
                    aprobado = aprobado,
                    usuarioId = usuarioId,
                    observaciones = observaciones
                )
            )
        }.body()
    }

    suspend fun exportLinks(filtros: KardexFilters): ExportLinks {
        val base = findWorkingUrl() ?: throw Exception("Servidor no disponible")
        val query = buildQueryString(filtros)
        val token = AuthManager.getToken().takeIf { it.isNotBlank() }?.let { "token=$it" }
        val queryWithToken = when {
            token == null -> query
            query.isBlank() -> "?$token"
            else -> "$query&$token"
        }
        return ExportLinks(
            csv = "$base$API_PATH/export/csv$queryWithToken",
            pdf = "$base$API_PATH/export/pdf$queryWithToken"
        )
    }

    private fun HttpRequestBuilder.applyFilters(filtros: KardexFilters) {
        filtros.productoId?.let { parameter("productoId", it) }
        filtros.tipo?.let { parameter("tipo", it.name) }
        filtros.estado?.let { parameter("estado", it.name) }
        filtros.desde?.takeIf { it.isNotBlank() }?.let { parameter("desde", it) }
        filtros.hasta?.takeIf { it.isNotBlank() }?.let { parameter("hasta", it) }
    }

    private fun buildQueryString(filtros: KardexFilters): String {
        val params = ParametersBuilder()
        filtros.productoId?.let { params.append("productoId", it.toString()) }
        filtros.tipo?.let { params.append("tipo", it.name) }
        filtros.estado?.let { params.append("estado", it.name) }
        filtros.desde?.takeIf { it.isNotBlank() }?.let { params.append("desde", it) }
        filtros.hasta?.takeIf { it.isNotBlank() }?.let { params.append("hasta", it) }
        val query = params.build().formUrlEncode()
        return if (query.isNotEmpty()) "?$query" else ""
    }

    fun close() {
        httpClient.close()
    }
}

data class ExportLinks(
    val csv: String,
    val pdf: String
)
