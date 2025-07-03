package org.example.project.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.example.project.data.repository.InventarioRepository
import org.example.project.data.model.ProductoRequest
import org.example.project.data.model.ProductoUI

class InventarioViewModel : ViewModel() {

    private val repository = InventarioRepository()

    // Estados del repositorio
    val productos = repository.productos
    val isLoading = repository.isLoading
    val error = repository.error

    // Estado de conexión
    private val _isConnected = MutableStateFlow<Boolean?>(null)
    val isConnected: StateFlow<Boolean?> = _isConnected.asStateFlow()

    // Estados locales para filtros y búsqueda
    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todas")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Productos filtrados basados en búsqueda y categoría
    val productosFiltrados: StateFlow<List<ProductoUI>> = combine(
        productos,
        searchText,
        selectedCategory
    ) { productos, search, category ->
        productos.filter { producto ->
            val matchesSearch = if (search.isEmpty()) {
                true
            } else {
                producto.nombre.contains(search, ignoreCase = true) ||
                producto.categoria.contains(search, ignoreCase = true)
            }

            val matchesCategory = category == "Todas" || producto.categoria == category

            matchesSearch && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Categorías disponibles
    val categorias: StateFlow<List<String>> = productos.map { productos ->
        listOf("Todas") + productos.map { it.categoria }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("Todas")
    )

    // Estadísticas calculadas
    val estadisticas: StateFlow<EstadisticasInventario> = productos.map { productos ->
        EstadisticasInventario(
            totalProductos = productos.size,
            productosBajoStock = productos.count { it.esBajoStock },
            productosAgotados = productos.count { it.stock == 0 },
            valorTotal = productos.sumOf { it.precio * it.stock }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EstadisticasInventario()
    )

    init {
        // Verificar conexión primero, luego cargar productos
        verificarConexion()
    }

    /**
     * Verificar conexión con el servidor
     */
    fun verificarConexion() {
        viewModelScope.launch {
            println("🔍 Verificando conexión con el servidor...")
            _isConnected.value = repository.verificarConexion()
            if (_isConnected.value == true) {
                println("✅ Conexión establecida, cargando productos...")
                cargarProductos()
            } else {
                println("❌ No se pudo conectar al servidor")
            }
        }
    }

    /**
     * Cargar productos desde la API
     */
    fun cargarProductos() {
        viewModelScope.launch {
            repository.cargarProductos()
        }
    }

    /**
     * Actualizar texto de búsqueda
     */
    fun updateSearchText(text: String) {
        _searchText.value = text
    }

    /**
     * Actualizar categoría seleccionada
     */
    fun updateSelectedCategory(category: String?) {
        _selectedCategory.value = category ?: "Todas"
    }

    /**
     * Agregar nuevo producto
     */
    fun agregarProducto(producto: ProductoRequest) {
        viewModelScope.launch {
            try {
                println("📦 Agregando producto: ${producto.nombre}")
                val nuevoProducto = repository.crearProducto(producto)
                if (nuevoProducto != null) {
                    println("✅ Producto agregado exitosamente")
                    cargarProductos() // Recargar lista
                } else {
                    println("❌ Error al agregar producto")
                }
            } catch (e: Exception) {
                println("❌ Error agregando producto: ${e.message}")
            }
        }
    }

    /**
     * Actualizar producto existente
     */
    fun actualizarProducto(id: Int, producto: ProductoRequest) {
        viewModelScope.launch {
            try {
                println("📝 Actualizando producto ID: $id")
                val productoActualizado = repository.actualizarProducto(id, producto)
                if (productoActualizado != null) {
                    println("✅ Producto actualizado exitosamente")
                    cargarProductos() // Recargar lista
                } else {
                    println("❌ Error al actualizar producto")
                }
            } catch (e: Exception) {
                println("❌ Error actualizando producto: ${e.message}")
            }
        }
    }

    /**
     * Eliminar producto
     */
    fun eliminarProducto(id: Int) {
        viewModelScope.launch {
            try {
                println("🗑️ Eliminando producto ID: $id")
                val eliminado = repository.eliminarProducto(id)
                if (eliminado) {
                    println("✅ Producto eliminado exitosamente")
                    cargarProductos() // Recargar lista
                } else {
                    println("❌ Error al eliminar producto")
                }
            } catch (e: Exception) {
                println("❌ Error eliminando producto: ${e.message}")
            }
        }
    }

    /**
     * Limpiar errores
     */
    fun limpiarError() {
        repository.limpiarError()
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}

/**
 * Clase para las estadísticas del inventario
 */
data class EstadisticasInventario(
    val totalProductos: Int = 0,
    val productosBajoStock: Int = 0,
    val productosAgotados: Int = 0,
    val valorTotal: Double = 0.0
)
