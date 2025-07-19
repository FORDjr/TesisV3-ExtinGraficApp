package org.example.project.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.project.data.models.*
import org.example.project.data.model.Producto

class VentasApiService(private val httpClient: HttpClient) {

    companion object {
        // CONFIGURACIÓN ACTUALIZADA PARA USAR SERVIDOR LOCAL FUNCIONANDO
        private const val LOCAL_WIFI_URL = "http://192.168.1.24:8090"        // Servidor local funcionando
        private const val LOCAL_RADMIN_URL = "http://26.36.148.66:8090"      // Radmin VPN backup
        private const val LOCALHOST_URL = "http://localhost:8090"            // localhost para desarrollo
        private const val EMULATOR_URL = "http://10.0.2.2:8090"             // Emulador Android
        private const val UNIVERSITY_URL = "http://146.83.198.35:1609"       // Servidor universidad (backup)

        private const val API_PATH = "/api"

        // Lista de URLs a probar - SERVIDOR LOCAL PRIMERO ya que está funcionando
        private val CONNECTION_URLS = listOf(
            LOCAL_WIFI_URL,     // Tu servidor local WiFi (funcionando)
            LOCAL_RADMIN_URL,   // Tu servidor Radmin VPN
            LOCALHOST_URL,      // localhost para desarrollo desktop
            EMULATOR_URL,       // Emulador Android
            UNIVERSITY_URL      // Servidor universidad como último recurso
        )

        // Instancia Json reutilizable
        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }

    // Función auxiliar para probar conexión con múltiples URLs
    private suspend fun tryRequest(path: String): Result<String> {
        val baseUrl = UNIVERSITY_URL // Usar directamente la URL de la universidad
        return try {
            val response = httpClient.get("$baseUrl$API_PATH$path")
            if (response.status == HttpStatusCode.OK) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Error HTTP: ${response.status}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Error de conexión: ${e.message}"))
        }
    }

    suspend fun obtenerVentas(): Result<List<Venta>> {
        val result = tryRequest("/ventas")
        return result.fold(
            onSuccess = { response ->
                try {
                    Result.success(json.decodeFromString(response))
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
        return try {
            val response = tryRequest("/ventas/metricas")
            response.fold(
                onSuccess = { jsonString ->
                    val metricas = json.decodeFromString<MetricasVentas>(jsonString)
                    Result.success(metricas)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun crearVenta(nuevaVenta: NuevaVentaRequest): Result<Venta> {
        return try {
            for (baseUrl in CONNECTION_URLS) {
                try {
                    val response = httpClient.post("$baseUrl$API_PATH/ventas") {
                        contentType(ContentType.Application.Json)
                        setBody(nuevaVenta)
                    }
                    if (response.status == HttpStatusCode.Created) {
                        val venta = response.body<Venta>()
                        return Result.success(venta)
                    }
                } catch (_: Exception) {
                    continue
                }
            }
            Result.failure(Exception("No se pudo crear la venta"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerVentaPorId(id: String): Result<Venta> {
        return try {
            val response = tryRequest("/ventas/$id")
            response.fold(
                onSuccess = { jsonString ->
                    val venta = json.decodeFromString<Venta>(jsonString)
                    Result.success(venta)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarEstadoVenta(id: String, nuevoEstado: EstadoVenta): Result<Venta> {
        return try {
            for (baseUrl in CONNECTION_URLS) {
                try {
                    val response = httpClient.patch("$baseUrl$API_PATH/ventas/$id/estado") {
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
        return try {
            println("🔍 VENTAS: Cargando productos desde servidor...")
            val response = tryRequest("/inventario/productos") // Usar el endpoint de inventario
            response.fold(
                onSuccess = { jsonString ->
                    val productos = json.decodeFromString<List<Producto>>(jsonString)
                    println("✅ VENTAS: ${productos.size} productos cargados exitosamente")
                    Result.success(productos)
                },
                onFailure = { error ->
                    println("❌ VENTAS: Error al obtener productos: ${error.message}")
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            println("❌ VENTAS: Excepción al cargar productos: ${e.message}")
            Result.failure(e)
        }
    }
}
