package org.example.project.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.example.project.data.models.*
import org.example.project.data.model.Producto

class VentasApiService {
    companion object {
        // Usar la misma configuración que InventarioApiService
        private const val UNIVERSITY_URL = "http://146.83.198.35:1609"
        private const val PRIMARY_URL = UNIVERSITY_URL
        private const val LOCALHOST_URL = "http://localhost:8080"
        private const val EMULATOR_URL = "http://10.0.2.2:8080"
        private const val API_PATH_VENTAS = "/api/ventas"
        private const val API_PATH_INVENTARIO = "/api/inventario"
        private val CONNECTION_URLS = listOf(
            PRIMARY_URL,
            LOCALHOST_URL,
            EMULATOR_URL
        )
        val json = Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        }
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(VentasApiService.json)
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }

    // Función auxiliar para probar conexión con múltiples URLs
    private suspend fun tryRequestVentas(path: String): Result<String> {
        val url = companionObjectWorkingUrl()
        return try {
            val response = httpClient.get("$url$API_PATH_VENTAS$path")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Error HTTP: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message}"))
        }
    }

    private suspend fun tryRequestInventario(path: String): Result<String> {
        val url = companionObjectWorkingUrl()
        return try {
            val response = httpClient.get("$url$API_PATH_INVENTARIO$path")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Error HTTP: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message}"))
        }
    }

    private fun companionObjectWorkingUrl(): String {
        // Usar la URL principal (universidad) como en InventarioApiService
        return PRIMARY_URL
    }

    suspend fun obtenerVentas(): Result<List<Venta>> {
        val result = tryRequestVentas("") // Llama a /api/ventas
        return result.fold(
            onSuccess = { response ->
                try {
                    Result.success(VentasApiService.json.decodeFromString<List<Venta>>(response))
                } catch (e: Exception) {
                    Result.failure(Exception("Error al decodificar respuesta: ${e.message}"))
                }
            },
            onFailure = { error ->
                Result.failure(Exception("Error al obtener ventas: ${error.message}"))
            }
        )
    }

    suspend fun obtenerMetricas(): Result<MetricasVentas> {
        val result = tryRequestVentas("/metricas") // Llama a /api/ventas/metricas
        return result.fold(
            onSuccess = { response ->
                try {
                    Result.success(VentasApiService.json.decodeFromString<MetricasVentas>(response))
                } catch (e: Exception) {
                    Result.failure(Exception("Error al decodificar métricas: ${e.message}"))
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    suspend fun crearVenta(nuevaVenta: NuevaVentaRequest): Result<Venta> {
        return try {
            for (baseUrl in CONNECTION_URLS) {
                try {
                    val response = httpClient.post("$baseUrl$API_PATH_VENTAS") {
                        contentType(ContentType.Application.Json)
                        setBody(nuevaVenta)
                    }
                    if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
                        val venta = response.body<Venta>()
                        return Result.success(venta)
                    }
                } catch (e: Exception) {
                    // Intenta con la siguiente URL
                }
            }
            Result.failure(Exception("No se pudo crear la venta en ningún servidor."))
        } catch (e: Exception) {
            Result.failure(Exception("Error al crear venta: ${e.message}"))
        }
    }

    suspend fun obtenerVentaPorId(id: String): Result<Venta> {
        val result = tryRequestVentas("/$id") // Llama a /api/ventas/{id}
        return result.fold(
            onSuccess = { response ->
                try {
                    Result.success(VentasApiService.json.decodeFromString<Venta>(response))
                } catch (e: Exception) {
                    Result.failure(Exception("Error al decodificar venta: ${e.message}"))
                }
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    suspend fun actualizarEstadoVenta(id: String, nuevoEstado: EstadoVenta): Result<Venta> {
        return try {
            for (baseUrl in CONNECTION_URLS) {
                try {
                    val response = httpClient.patch("$baseUrl$API_PATH_VENTAS/ventas/$id/estado") {
                        contentType(ContentType.Application.Json)
                        setBody(mapOf("estado" to nuevoEstado))
                    }
                    if (response.status == HttpStatusCode.OK) {
                        val venta = response.body<Venta>()
                        return Result.success(venta)
                    }
                } catch (_: Exception) {
                    continue
                }
            }
            Result.failure(Exception("No se pudo actualizar el estado de la venta"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerProductosParaVenta(): Result<List<Producto>> {
        for (baseUrl in CONNECTION_URLS) {
            try {
                val response = httpClient.get("$baseUrl$API_PATH_INVENTARIO")
                if (response.status == HttpStatusCode.OK) {
                    val productos = response.body<List<Producto>>()
                    println("✅ VENTAS: ${productos.size} productos cargados exitosamente desde $baseUrl$API_PATH_INVENTARIO")
                    return Result.success(productos)
                }
            } catch (e: Exception) {
                println("❌ VENTAS: Error al obtener productos desde $baseUrl$API_PATH_INVENTARIO: ${e.message}")
                continue
            }
        }
        return Result.failure(Exception("No se pudo obtener productos desde ningún servidor."))
    }
}
