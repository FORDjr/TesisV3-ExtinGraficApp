package org.example.project.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.data.api.InventarioApiService
import org.example.project.data.model.Producto
import org.example.project.data.model.ProductoRequest
import org.example.project.data.model.toUI
import org.example.project.data.model.ProductoUI

class InventarioRepository {

    private val apiService = InventarioApiService()

    // Estado reactivo de la lista de productos
    private val _productos = MutableStateFlow<List<ProductoUI>>(emptyList())
    val productos: StateFlow<List<ProductoUI>> = _productos.asStateFlow()

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Estado de error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Verificar conexión con el servidor
     */
    suspend fun verificarConexion(): Boolean {
        return try {
            _isLoading.value = true
            _error.value = null
            apiService.verificarConexion()
        } catch (e: Exception) {
            _error.value = "Error de conexión: ${e.message}"
            false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Cargar todos los productos desde la API
     */
    suspend fun cargarProductos() {
        try {
            _isLoading.value = true
            _error.value = null

            val productosApi = apiService.obtenerProductos()
            val productosUI = productosApi.map { it.toUI() }

            _productos.value = productosUI

            println("✅ Productos cargados exitosamente: ${productosUI.size} productos")

        } catch (e: Exception) {
            _error.value = "Error al cargar productos: ${e.message}"
            println("❌ Error al cargar productos: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Crear un nuevo producto
     */
    suspend fun crearProducto(productoRequest: ProductoRequest): Boolean {
        return try {
            _isLoading.value = true
            _error.value = null

            val nuevoProducto = apiService.crearProducto(productoRequest)

            if (nuevoProducto != null) {
                // Recargar la lista para mostrar el nuevo producto
                cargarProductos()
                println("✅ Producto creado exitosamente: ${nuevoProducto.nombre}")
                true
            } else {
                _error.value = "Error al crear el producto"
                false
            }

        } catch (e: Exception) {
            _error.value = "Error al crear producto: ${e.message}"
            println("❌ Error al crear producto: ${e.message}")
            false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Actualizar un producto existente
     */
    suspend fun actualizarProducto(id: Int, productoRequest: ProductoRequest): Boolean {
        return try {
            _isLoading.value = true
            _error.value = null

            val productoActualizado = apiService.actualizarProducto(id, productoRequest)

            if (productoActualizado != null) {
                // Recargar la lista para mostrar los cambios
                cargarProductos()
                println("✅ Producto actualizado exitosamente: ${productoActualizado.nombre}")
                true
            } else {
                _error.value = "Error al actualizar el producto"
                false
            }

        } catch (e: Exception) {
            _error.value = "Error al actualizar producto: ${e.message}"
            println("❌ Error al actualizar producto: ${e.message}")
            false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Actualizar solo el stock de un producto
     */
    suspend fun actualizarStock(id: Int, cantidad: Int): Boolean {
        return try {
            _isLoading.value = true
            _error.value = null

            val productoActualizado = apiService.actualizarStock(id, cantidad)

            if (productoActualizado != null) {
                // Actualizar solo el producto específico en la lista local
                val productosActuales = _productos.value.toMutableList()
                val indice = productosActuales.indexOfFirst { it.id == id }

                if (indice != -1) {
                    productosActuales[indice] = productoActualizado.toUI()
                    _productos.value = productosActuales
                }

                println("✅ Stock actualizado exitosamente para producto ID: $id")
                true
            } else {
                _error.value = "Error al actualizar el stock"
                false
            }

        } catch (e: Exception) {
            _error.value = "Error al actualizar stock: ${e.message}"
            println("❌ Error al actualizar stock: ${e.message}")
            false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Eliminar un producto
     */
    suspend fun eliminarProducto(id: Int): Boolean {
        return try {
            _isLoading.value = true
            _error.value = null

            val eliminado = apiService.eliminarProducto(id)

            if (eliminado) {
                // Eliminar el producto de la lista local
                val productosActuales = _productos.value.toMutableList()
                productosActuales.removeAll { it.id == id }
                _productos.value = productosActuales

                println("✅ Producto eliminado exitosamente ID: $id")
                true
            } else {
                _error.value = "Error al eliminar el producto"
                false
            }

        } catch (e: Exception) {
            _error.value = "Error al eliminar producto: ${e.message}"
            println("❌ Error al eliminar producto: ${e.message}")
            false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Limpiar errores
     */
    fun limpiarError() {
        _error.value = null
    }

    /**
     * Liberar recursos
     */
    fun close() {
        apiService.close()
    }
}
