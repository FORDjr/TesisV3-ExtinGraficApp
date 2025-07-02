package org.example.project.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.example.project.data.model.Producto
import org.example.project.data.model.ProductoRequest

class InventarioApiService {

    companion object {
        // Configuración para diferentes entornos
        private const val EMULATOR_URL = "http://10.0.2.2:8080"
        private const val LOCALHOST_URL = "http://localhost:8080"
        private const val API_PATH = "/api/inventario"

        // URL base que se puede cambiar según el entorno
        private const val BASE_URL = EMULATOR_URL
    }

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    /**
     * Verificar conexión con el servidor
     */
    suspend fun verificarConexion(): Boolean {
        return try {
            println("🔍 Verificando conexión a: $BASE_URL/health")
            val response = httpClient.get("$BASE_URL/health")
            val isSuccess = response.status.isSuccess()
            println("✅ Conexión ${if (isSuccess) "exitosa" else "fallida"}: ${response.status}")
            isSuccess
        } catch (e: Exception) {
            println("❌ Error de conexión: ${e.message}")
            false
        }
    }

    /**
     * Obtener todos los productos del inventario
     */
    suspend fun obtenerProductos(): List<Producto> {
        return try {
            println("🔄 Obteniendo productos desde: $BASE_URL$API_PATH")
            val productos = httpClient.get("$BASE_URL$API_PATH").body<List<Producto>>()
            println("✅ Productos obtenidos: ${productos.size}")
            productos
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
            httpClient.get("$BASE_URL$API_PATH/$id").body()
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
            httpClient.post("$BASE_URL$API_PATH") {
                contentType(ContentType.Application.Json)
                setBody(producto)
            }.body()
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
            httpClient.put("$BASE_URL$API_PATH/$id") {
                contentType(ContentType.Application.Json)
                setBody(producto)
            }.body()
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
            httpClient.patch("$BASE_URL$API_PATH/$id/stock") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("cantidad" to cantidad))
            }.body()
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
            val response = httpClient.delete("$BASE_URL$API_PATH/$id")
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Error al eliminar producto $id: ${e.message}")
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
