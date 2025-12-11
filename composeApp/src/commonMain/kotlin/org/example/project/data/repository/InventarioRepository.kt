package org.example.project.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.data.api.InventarioApiService
import org.example.project.data.model.EstadoProductoRemote
import org.example.project.data.model.InventarioQuery
import org.example.project.data.model.ProductoRequest
import org.example.project.data.model.toUI
import org.example.project.data.model.ProductoUI
import org.example.project.data.sync.PendingSyncManager
import org.example.project.utils.NotificationPreferences
import org.example.project.utils.Notifier

class InventarioRepository {

    private val apiService = InventarioApiService()

    private val _productos = MutableStateFlow<List<ProductoUI>>(emptyList())
    val productos: StateFlow<List<ProductoUI>> = _productos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _categorias = MutableStateFlow<List<String>>(emptyList())
    val categorias: StateFlow<List<String>> = _categorias.asStateFlow()

    // Nuevo: estado modo offline
    private val _offlineMode = MutableStateFlow(false)
    val offlineMode: StateFlow<Boolean> = _offlineMode.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val pageSize = 20
    private var currentOffset = 0
    private var currentQuery: InventarioQuery = InventarioQuery()

    /**
     * Verificar conexión con el servidor
     */
    suspend fun verificarConexion(): Boolean {
        return try {
            _isLoading.value = true
            _error.value = null
            val ok = apiService.verificarConexion()
            if (ok) {
                _offlineMode.value = false
                runCatching { PendingSyncManager.processQueue() }
            }
            ok
        } catch (e: Exception) {
            _error.value = "Error de conexión: ${e.message}"
            _offlineMode.value = true
            false
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Cargar todos los productos desde la API
     */
    suspend fun cargarProductos(
        query: InventarioQuery = currentQuery,
        reset: Boolean = true
    ) {
        try {
            if (reset) {
                currentQuery = query
                currentOffset = 0
                _productos.value = emptyList()
                _hasMore.value = false
            }
            _isLoading.value = true
            _error.value = null

            println("🔄 Intentando cargar productos desde servidor...")

            val page = apiService.obtenerProductos(currentQuery, pageSize, currentOffset)
            val productosUI = page.items
                .sortedByDescending { it.fechaActualizacion }
                .map { it.toUI() }

            _productos.value = if (reset) {
                productosUI
            } else {
                val merged = _productos.value.associateBy { it.id }.toMutableMap()
                productosUI.forEach { merged[it.id] = it }
                merged.values.sortedByDescending { it.fechaIngreso }
            }
            currentOffset += page.items.size
            _hasMore.value = page.hasMore
            _offlineMode.value = false

            actualizarCategoriasDesdeProductos()

            println("✅ Productos cargados: ${_productos.value.size} (pagina=${page.items.size})")

        } catch (e: Exception) {
            _error.value = e.message ?: "Error desconocido al cargar productos"
            _offlineMode.value = true
            _hasMore.value = false
            println("❌ Error cargando productos: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun cargarMas() {
        if (!_hasMore.value || _isLoading.value) return
        cargarProductos(currentQuery, reset = false)
    }

    /**
     * Crear un nuevo producto
     */
    suspend fun crearProducto(productoRequest: ProductoRequest): Boolean {
        return try {
            _isLoading.value = true
            _error.value = null

            val exito = apiService.crearProducto(productoRequest)
            if (exito) {
                cargarProductos(reset = true)
                // Reconsultar categorías para reflejar nuevas
                cargarCategorias()
                println("✅ Producto creado")
                true
            } else {
                _error.value = "Error al crear producto"
                false
            }
        } catch (e: Exception) {
            _error.value = "Error al crear producto: ${e.message}"
            println("❌ ${e.message}")
            false
        } finally { _isLoading.value = false }
    }

    /**
     * Actualizar un producto existente
     */
    suspend fun actualizarProducto(id: Int, productoRequest: ProductoRequest): Boolean {
        return try {
            _isLoading.value = true
            _error.value = null

            val exito = apiService.actualizarProducto(id, productoRequest)
            if (exito) {
                cargarProductos(reset = true)
                println("✅ Producto actualizado ($id)")
                true
            } else {
                _error.value = "Error al actualizar producto"
                false
            }
        } catch (e: Exception) {
            _error.value = "Error al actualizar producto: ${e.message}"
            println("❌ ${e.message}")
            false
        } finally { _isLoading.value = false }
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
                val productoUI = productoActualizado.toUI()
                // Actualizar solo el producto específico en la lista local
                val lista = _productos.value.toMutableList()
                val idx = lista.indexOfFirst { it.id == id }

                if (idx != -1) {
                    lista[idx] = productoUI
                } else {
                    lista.add(productoUI)
                }
                _productos.value = lista
                actualizarCategoriasDesdeProductos()

                if (NotificationPreferences.settings.value.notifyStock && productoUI.esBajoStock) {
                    Notifier.notify("Stock crítico", "${productoUI.nombre} en ${productoUI.stock} unidades")
                }

                true
            } else {
                _error.value = "Error al actualizar stock"
                false
            }
        } catch (e: Exception) {
            _error.value = "Error al actualizar stock: ${e.message}"
            false
        } finally { _isLoading.value = false }
    }

    suspend fun obtenerCriticos(): List<ProductoUI> {
        return try {
            apiService.obtenerCriticos().map { it.toUI() }
        } catch (e: Exception) {
            _error.value = e.message
            emptyList()
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
                // Refrescar lista para respetar filtros de estado
                cargarProductos(reset = true)
                true
            } else {
                _error.value = "Error al eliminar producto"
                false
            }
        } catch (e: Exception) {
            _error.value = "Error al eliminar producto: ${e.message}"
            false
        } finally { _isLoading.value = false }
    }

    /**
     * Cargar categorías desde la API
     */
    suspend fun cargarCategorias() {
        try {
            _isLoading.value = true
            _error.value = null
            val cats = apiService.obtenerCategorias()
            _categorias.value = cats
            _offlineMode.value = false
        } catch (e: Exception) {
            actualizarCategoriasDesdeProductos()
            _error.value = "Error al cargar categorías: ${e.message}"
        } finally { _isLoading.value = false }
    }

    /**
     * Limpiar errores
     */
    fun limpiarError() { _error.value = null }

    /**
     * Liberar recursos
     */
    fun close() { apiService.close() }

    /**
     * Establecer categorías directamente (para actualización desde ViewModel)
     */
    fun setCategorias(categorias: List<String>) { _categorias.value = categorias }

    private fun actualizarCategoriasDesdeProductos() {
        val categoriasFromProducts = _productos.value
            .mapNotNull { it.categoria.takeIf { cat -> cat.isNotBlank() } }
            .distinct()
        if (categoriasFromProducts.isNotEmpty()) {
            _categorias.value = categoriasFromProducts
        }
    }
}
