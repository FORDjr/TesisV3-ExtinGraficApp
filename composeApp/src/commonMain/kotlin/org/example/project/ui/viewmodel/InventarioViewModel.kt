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

    // DATOS DEMO INMEDIATOS - SIEMPRE DISPONIBLES
    private val productosDemo = listOf(
        ProductoUI(
            id = 1,
            nombre = "Extintor PQS 6kg",
            categoria = "Extintores",
            precio = 45000.0,
            stock = 15,
            stockMinimo = 5,
            descripcion = "Extintor de polvo químico seco para fuegos ABC",
            fechaIngreso = "2025-01-03"
        ),
        ProductoUI(
            id = 2,
            nombre = "Extintor CO2 5kg",
            categoria = "Extintores",
            precio = 65000.0,
            stock = 8,
            stockMinimo = 3,
            descripcion = "Extintor de dióxido de carbono para fuegos BC",
            fechaIngreso = "2025-01-03"
        ),
        ProductoUI(
            id = 3,
            nombre = "Detector de Humo",
            categoria = "Detectores",
            precio = 25000.0,
            stock = 25,
            stockMinimo = 10,
            descripcion = "Detector de humo fotoeléctrico",
            fechaIngreso = "2025-01-03"
        ),
        ProductoUI(
            id = 4,
            nombre = "Manguera Contraincendios",
            categoria = "Accesorios",
            precio = 120000.0,
            stock = 5,
            stockMinimo = 2,
            descripcion = "Manguera de 25m para sistemas contraincendios",
            fechaIngreso = "2025-01-03"
        ),
        ProductoUI(
            id = 5,
            nombre = "Señalética Salida Emergencia",
            categoria = "Señalización",
            precio = 8000.0,
            stock = 50,
            stockMinimo = 20,
            descripcion = "Señal luminosa de salida de emergencia",
            fechaIngreso = "2025-01-03"
        ),
        ProductoUI(
            id = 6,
            nombre = "Extintor H2O 9L",
            categoria = "Extintores",
            precio = 35000.0,
            stock = 2,
            stockMinimo = 5,
            descripcion = "Extintor de agua para fuegos tipo A",
            fechaIngreso = "2025-01-03"
        )
    )

    // Override productos para mostrar datos demo SOLO cuando no hay conexión
    val productosFinales: StateFlow<List<ProductoUI>> = combine(
        productos,
        isConnected
    ) { productosRepo, conexionEstado ->
        when {
            // Si hay conexión y productos del servidor, usar esos
            conexionEstado == true && productosRepo.isNotEmpty() -> {
                println("✅ Modo ONLINE: Usando ${productosRepo.size} productos del servidor")
                productosRepo
            }
            // Si hay conexión pero no hay productos aún, mostrar lista vacía (cargando)
            conexionEstado == true && productosRepo.isEmpty() -> {
                println("🔄 Modo ONLINE: Cargando productos del servidor...")
                emptyList()
            }
            // Si NO hay conexión, usar datos demo
            conexionEstado == false -> {
                println("📱 Modo OFFLINE: Usando ${productosDemo.size} productos demo")
                productosDemo
            }
            // Estado inicial (null) - mostrar datos demo temporalmente
            else -> {
                println("🔄 Estado inicial: Usando datos demo mientras se verifica conexión...")
                productosDemo
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList() // Empezar vacío hasta verificar conexión
    )

    // Productos filtrados basados en búsqueda y categoría - AHORA USANDO productosFinales
    val productosFiltrados: StateFlow<List<ProductoUI>> = combine(
        productosFinales, // CAMBIO CLAVE: usar productosFinales en lugar de productos
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
        initialValue = productosDemo // IMPORTANTE: Inicializar con datos demo
    )

    // Categorías disponibles
    val categorias: StateFlow<List<String>> = productos.map { productos ->
        listOf("Todas") + productos.map { it.categoria }.distinct().sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("Todas")
    )

    // Estadísticas calculadas - AHORA USANDO productosFinales
    val estadisticas: StateFlow<EstadisticasInventario> = productosFinales.map { productos ->
        EstadisticasInventario(
            totalProductos = productos.size,
            productosBajoStock = productos.count { it.esBajoStock },
            productosAgotados = productos.count { it.stock == 0 },
            valorTotal = productos.sumOf { it.precio * it.stock }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = EstadisticasInventario(
            totalProductos = 6,
            productosBajoStock = 1,
            productosAgotados = 0,
            valorTotal = 303000.0
        )
    )

    init {
        // Cargar datos demo inmediatamente al inicializar, luego intentar servidor
        println("🔄 Inicializando InventarioViewModel...")
        viewModelScope.launch {
            // Primero cargar datos demo como fallback inmediato
            cargarDatosDemoDirecto()

            // Después intentar conectar al servidor
            verificarConexion()
        }
    }

    /**
     * Cargar datos demo directamente en el ViewModel
     */
    private fun cargarDatosDemoDirecto() {
        println("🔄 Cargando datos demo directamente...")
        val fechaHoy = "2025-01-03"

        val productosDemo = listOf(
            ProductoUI(
                id = 1,
                nombre = "Extintor PQS 6kg",
                categoria = "Extintores",
                precio = 45000.0,
                stock = 15,
                stockMinimo = 5,
                descripcion = "Extintor de polvo químico seco para fuegos ABC",
                fechaIngreso = fechaHoy
            ),
            ProductoUI(
                id = 2,
                nombre = "Extintor CO2 5kg",
                categoria = "Extintores",
                precio = 65000.0,
                stock = 8,
                stockMinimo = 3,
                descripcion = "Extintor de dióxido de carbono para fuegos BC",
                fechaIngreso = fechaHoy
            ),
            ProductoUI(
                id = 3,
                nombre = "Detector de Humo",
                categoria = "Detectores",
                precio = 25000.0,
                stock = 25,
                stockMinimo = 10,
                descripcion = "Detector de humo fotoeléctrico",
                fechaIngreso = fechaHoy
            ),
            ProductoUI(
                id = 4,
                nombre = "Manguera Contraincendios",
                categoria = "Accesorios",
                precio = 120000.0,
                stock = 5,
                stockMinimo = 2,
                descripcion = "Manguera de 25m para sistemas contraincendios",
                fechaIngreso = fechaHoy
            ),
            ProductoUI(
                id = 5,
                nombre = "Señalética Salida Emergencia",
                categoria = "Señalización",
                precio = 8000.0,
                stock = 50,
                stockMinimo = 20,
                descripcion = "Señal luminosa de salida de emergencia",
                fechaIngreso = fechaHoy
            ),
            ProductoUI(
                id = 6,
                nombre = "Extintor H2O 9L",
                categoria = "Extintores",
                precio = 35000.0,
                stock = 2,
                stockMinimo = 5,
                descripcion = "Extintor de agua para fuegos tipo A",
                fechaIngreso = fechaHoy
            )
        )

        // Forzar la actualización directa en el repositorio
        repository.setProductosDemo(productosDemo)
        println("✅ Datos demo cargados directamente: ${productosDemo.size} productos")
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
                println("❌ No se pudo conectar al servidor, cargando datos demo...")
                // IMPORTANTE: Aunque no haya conexión, intentar cargar productos (activará modo demo)
                cargarProductos()
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
                val exitoso = repository.crearProducto(producto)
                if (exitoso) {
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
                val exitoso = repository.actualizarProducto(id, producto)
                if (exitoso) {
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
