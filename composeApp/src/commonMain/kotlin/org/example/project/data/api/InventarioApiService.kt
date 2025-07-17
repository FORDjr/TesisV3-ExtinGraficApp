package org.example.project.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.project.data.model.Producto
import org.example.project.data.model.ProductoRequest
import org.example.project.*

class InventarioApiService {

    companion object {
        // URL del servidor universitario - Configuración para producción
        private const val UNIVERSITY_URL = "http://146.83.198.35:1609" // Servidor universidad
        private const val PRIMARY_URL = UNIVERSITY_URL      // Usar servidor universitario como primario
        private const val LOCALHOST_URL = "http://localhost:8080"    // Para desarrollo desktop
        private const val EMULATOR_URL = "http://10.0.2.2:8080"      // Para emulador Android local

        private const val API_PATH = "/api/inventario"

        // Lista de URLs a probar - primero universidad, luego localhost, luego emulador
        private val CONNECTION_URLS = listOf(
            PRIMARY_URL,        // Servidor universidad primero
            LOCALHOST_URL,      // localhost para desarrollo desktop
            EMULATOR_URL        // Emulador Android como respaldo
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
            requestTimeoutMillis = 15000L  // Reducido a 15 segundos
            connectTimeoutMillis = 10000L  // Reducido a 10 segundos
            socketTimeoutMillis = 15000L   // Reducido a 15 segundos
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }

    private var workingUrl: String? = null

    /**
     * Encuentra la URL que funciona probando todas las opciones
     */
    private suspend fun findWorkingUrl(): String? {
        // Si ya tenemos una URL que funciona, verificarla rápidamente
        workingUrl?.let { url ->
            if (testConnectionQuick(url)) {
                return url
            } else {
                workingUrl = null // Invalidar URL que ya no funciona
            }
        }

        // Probar todas las URLs hasta encontrar una que funcione
        for (url in CONNECTION_URLS) {
            println("🔍 Probando conexión a: $url")
            if (testConnection(url)) {
                workingUrl = url
                println("✅ Conexión exitosa con: $url")
                return url
            }
        }

        println("❌ No se pudo establecer conexión con ningún servidor")
        return null
    }

    /**
     * Prueba rápida de conexión (sin health endpoint)
     */
    private suspend fun testConnectionQuick(url: String): Boolean {
        return try {
            val response = httpClient.get("$url$API_PATH")
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Probar conexión con una URL específica
     */
    private suspend fun testConnection(url: String): Boolean {
        return try {
            // Primero probar el endpoint health si existe
            try {
                val healthResponse = httpClient.get("$url/health")
                if (healthResponse.status.isSuccess()) {
                    return true
                }
            } catch (e: Exception) {
                // Si no hay endpoint health, continuar con el API del inventario
            }

            // Probar directamente el API del inventario
            val response = httpClient.get("$url$API_PATH")
            response.status.isSuccess()
        } catch (e: Exception) {
            println("❌ Error probando $url: ${e.message}")
            false
        }
    }

    /**
     * Verificar conexión con el servidor
     */
    suspend fun verificarConexion(): Boolean {
        return findWorkingUrl() != null
    }

    /**
     * Realizar petición HTTP con fallback automático
     */
    private suspend fun <T> makeRequest(block: suspend (String) -> T): T {
        val url = findWorkingUrl()
            ?: throw Exception("No se pudo establecer conexión con el servidor")

        return try {
            block(url)
        } catch (e: Exception) {
            // Si falla, invalidar la URL actual y buscar otra
            workingUrl = null
            val newUrl = findWorkingUrl()
                ?: throw Exception("Perdió conexión con el servidor")
            block(newUrl)
        }
    }

    /**
     * Obtener todos los productos del inventario
     */
    suspend fun obtenerProductos(): List<Producto> {
        return try {
            makeRequest { url ->
                println("���� Obteniendo productos desde: $url$API_PATH")
                val productos = httpClient.get("$url$API_PATH").body<List<Producto>>()
                println("✅ Productos obtenidos: ${productos.size}")
                productos
            }
        } catch (e: Exception) {
            println("❌ Error al obtener productos: ${e.message}")
            emptyList()
        }
    }

    /**
     * Obtener un producto por ID
     */
    suspend fun obtenerProductoPorId(id: Int): Producto? {
        return try {
            makeRequest { url ->
                httpClient.get("$url$API_PATH/$id").body()
            }
        } catch (e: Exception) {
            println("Error al obtener producto $id: ${e.message}")
            null
        }
    }

    /**
     * Crear un nuevo producto
     */
    suspend fun crearProducto(producto: ProductoRequest): Producto? {
        return try {
            makeRequest { url ->
                httpClient.post("$url$API_PATH") {
                    contentType(ContentType.Application.Json)
                    setBody(producto)
                }.body()
            }
        } catch (e: Exception) {
            println("Error al crear producto: ${e.message}")
            null
        }
    }

    /**
     * Actualizar un producto existente
     */
    suspend fun actualizarProducto(id: Int, producto: ProductoRequest): Producto? {
        return try {
            makeRequest { url ->
                httpClient.put("$url$API_PATH/$id") {
                    contentType(ContentType.Application.Json)
                    setBody(producto)
                }.body()
            }
        } catch (e: Exception) {
            println("Error al actualizar producto $id: ${e.message}")
            null
        }
    }

    /**
     * Actualizar solo el stock de un producto
     */
    suspend fun actualizarStock(id: Int, cantidad: Int): Producto? {
        return try {
            makeRequest { url ->
                httpClient.patch("$url$API_PATH/$id/stock") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("cantidad" to cantidad))
                }.body()
            }
        } catch (e: Exception) {
            println("Error al actualizar stock del producto $id: ${e.message}")
            null
        }
    }

    /**
     * Eliminar un producto
     */
    suspend fun eliminarProducto(id: Int): Boolean {
        return try {
            makeRequest { url ->
                val response = httpClient.delete("$url$API_PATH/$id")
                response.status.isSuccess()
            }
        } catch (e: Exception) {
            println("Error al eliminar producto $id: ${e.message}")
            false
        }
    }

    /**
     * Prueba simple de conexión (método básico)
     */
    suspend fun probarConexion(): Boolean {
        return try {
            val response = httpClient.get("http://192.168.1.24:8090/test")
            response.status.isSuccess()
        } catch (e: Exception) {
            println("❌ Prueba falló: ${e.message}")
            false
        }
    }

    /**
     * Cerrar el cliente HTTP cuando ya no se necesite
     */
    fun close() {
        httpClient.close()
    }
}
