package org.example.project.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.example.project.data.repository.VentasRepository
import org.example.project.data.repository.InventarioRepository
import org.example.project.data.repository.MercadoLibreRepository
import org.example.project.data.models.*
import org.example.project.data.model.Producto
import org.example.project.data.model.toUI

class VentasViewModel(
    private val ventasRepository: VentasRepository,
    private val inventarioRepository: InventarioRepository,
    private val mercadoLibreRepository: MercadoLibreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VentasUiState())
    val uiState: StateFlow<VentasUiState> = _uiState.asStateFlow()

    private val _nuevaVentaState = MutableStateFlow(NuevaVentaUiState())
    val nuevaVentaState: StateFlow<NuevaVentaUiState> = _nuevaVentaState.asStateFlow()
    private val _mercadoLibreAssignmentState = MutableStateFlow(MercadoLibreAssignmentUiState())
    val mercadoLibreAssignmentState: StateFlow<MercadoLibreAssignmentUiState> = _mercadoLibreAssignmentState.asStateFlow()


    // Nuevo estado para crear productos
    private val _nuevoProductoState = MutableStateFlow(NuevoProductoUiState())
    val nuevoProductoState: StateFlow<NuevoProductoUiState> = _nuevoProductoState.asStateFlow()

    private val demoClientes = listOf(
        "Camila Reyes",
        "Javier Muñoz",
        "Sofía Torres",
        "Diego Silva",
        "Valentina Pérez",
        "Mateo Rojas",
        "Antonia Campos",
        "Lucas Herrera"
    )
    private val demoPagos = listOf(MetodoPago.EFECTIVO, MetodoPago.TARJETA, MetodoPago.TRANSFERENCIA, MetodoPago.CREDITO)

    private fun ventasConClientesReales(ventas: List<Venta>): List<Venta> {
        var idx = 0
        return ventas.map { venta ->
            val needsReplacement = venta.cliente.isBlank() || venta.cliente.startsWith("Cliente Final", ignoreCase = true)
            if (needsReplacement) {
                val nombre = demoClientes[idx % demoClientes.size]
                idx++
                venta.copy(cliente = nombre)
            } else venta
        }
    }

    private fun ventasDemo(): List<Venta> {
        val now = "2025-01-15T12:00:00Z"
        return demoClientes.take(5).mapIndexed { i, nombre ->
            Venta(
                id = "V${100 + i}",
                cliente = nombre,
                fecha = now,
                total = 45000L + (i * 7500L),
                estado = if (i % 2 == 0) EstadoVenta.COMPLETADA else EstadoVenta.PENDIENTE,
                metodoPago = demoPagos[i % demoPagos.size],
                productos = listOf(
                    ProductoVenta(
                        id = i + 1,
                        nombre = "Producto ${i + 1}",
                        cantidad = 1 + (i % 3),
                        precio = 15000L,
                        subtotal = (1 + (i % 3)) * 15000L
                    )
                )
            )
        }
    }

    init {
        observeMercadoLibre()
        cargarDatos()
        viewModelScope.launch {
            if (inventarioRepository.productos.value.isEmpty()) {
                try {
                    inventarioRepository.cargarProductos()
                } catch (_: Exception) {
                    // Ignoramos porque el repositorio ya maneja fallback demo.
                }
            }
        }
        sincronizarMercadoLibre()
    }

    private fun observeMercadoLibre() {
        viewModelScope.launch {
            combine(
                mercadoLibreRepository.orders,
                mercadoLibreRepository.isLoading,
                mercadoLibreRepository.error
            ) { orders, loading, error ->
                _uiState.update { state ->
                    state.copy(
                        mercadoLibreOrders = orders,
                        mercadoLibreIsLoading = loading,
                        mercadoLibreError = error
                    )
                }
            }.collect()
        }
    }

    fun sincronizarMercadoLibre() {
        viewModelScope.launch {
            mercadoLibreRepository.syncLatestOrders()
        }
    }

    fun abrirAsignacionMercadoLibre(orderId: String) {
        val order = mercadoLibreRepository.orders.value.firstOrNull { it.id == orderId } ?: return
        viewModelScope.launch {
            if (inventarioRepository.productos.value.isEmpty()) {
                try {
                    inventarioRepository.cargarProductos()
                } catch (_: Exception) {
                    // modo demo se encargara si la red falla
                }
            }
            val productos = inventarioRepository.productos.value
            val defaultQuantity = order.items.sumOf { it.quantity }.coerceAtLeast(1)
            _mercadoLibreAssignmentState.value = MercadoLibreAssignmentUiState(
                isDialogOpen = true,
                selectedOrder = order,
                availableProducts = productos,
                selectedProduct = productos.firstOrNull(),
                quantity = defaultQuantity
            )
        }
    }

    fun cerrarAsignacionMercadoLibre() {
        _mercadoLibreAssignmentState.value = MercadoLibreAssignmentUiState()
    }

    fun seleccionarProductoAsignacion(productId: Int) {
        val state = _mercadoLibreAssignmentState.value
        val producto = state.availableProducts.firstOrNull { it.id == productId } ?: return
        val safeQuantity = state.quantity.coerceAtMost(producto.stock.coerceAtLeast(0))
        _mercadoLibreAssignmentState.value = state.copy(
            selectedProduct = producto,
            quantity = safeQuantity.coerceAtLeast(1),
            error = null
        )
    }

    fun actualizarCantidadAsignacion(nuevaCantidad: Int) {
        val state = _mercadoLibreAssignmentState.value
        if (!state.isDialogOpen || nuevaCantidad <= 0) return
        val maxCantidad = state.selectedProduct?.stock ?: nuevaCantidad
        val ajustada = nuevaCantidad.coerceAtMost(maxCantidad.coerceAtLeast(1))
        _mercadoLibreAssignmentState.value = state.copy(quantity = ajustada, error = null)
    }

    fun confirmarAsignacionMercadoLibre() {
        val state = _mercadoLibreAssignmentState.value
        val order = state.selectedOrder ?: run {
            _mercadoLibreAssignmentState.value = state.copy(error = "Selecciona una orden valida")
            return
        }
        val producto = state.selectedProduct ?: run {
            _mercadoLibreAssignmentState.value = state.copy(error = "Selecciona un producto del inventario")
            return
        }
        val cantidad = state.quantity
        if (cantidad <= 0) {
            _mercadoLibreAssignmentState.value = state.copy(error = "La cantidad debe ser mayor a cero")
            return
        }
        viewModelScope.launch {
            val inventarioActual = inventarioRepository.productos.value.firstOrNull { it.id == producto.id } ?: producto
            if (cantidad > inventarioActual.stock) {
                _mercadoLibreAssignmentState.value = state.copy(
                    error = "Stock insuficiente para ${producto.nombre}"
                )
                return@launch
            }
            _mercadoLibreAssignmentState.value = state.copy(isProcessing = true, error = null)
            val nuevoStock = (inventarioActual.stock - cantidad).coerceAtLeast(0)
            val actualizado = inventarioRepository.actualizarStock(producto.id, nuevoStock)
            if (actualizado) {
                mercadoLibreRepository.registerAssignment(
                    orderId = order.id,
                    assignment = MercadoLibreAssignment(
                        productId = producto.id,
                        productName = producto.nombre,
                        quantity = cantidad
                    )
                )
                _mercadoLibreAssignmentState.value = MercadoLibreAssignmentUiState()
                sincronizarMercadoLibre()
            } else {
                _mercadoLibreAssignmentState.value = state.copy(
                    isProcessing = false,
                    error = "No se pudo actualizar el stock"
                )
            }
        }
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                ventasRepository.obtenerVentasResumen().collect { resultado ->
                    resultado.fold(
                        onSuccess = { resumen ->
                            val ventasLimpias = ventasConClientesReales(resumen.ventas)
                            _uiState.value = _uiState.value.copy(
                                ventas = ventasLimpias,
                                metricas = resumen.metricas,
                                isLoading = false,
                                error = null
                            )
                            _uiState.value = _uiState.value.copy(
                                ventas = _uiState.value.ventas.sortedByDescending { it.fecha }
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Error al cargar ventas: ${error.message}",
                                ventas = ventasDemo(),
                                metricas = ventasRepository.obtenerMetricasEjemplo()
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error inesperado: ${e.message}",
                    ventas = ventasDemo(),
                    metricas = ventasRepository.obtenerMetricasEjemplo()
                )
            }
        }
    }

    fun buscarVentas(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun filtrarPorEstado(estado: EstadoVenta?) {
        _uiState.value = _uiState.value.copy(filtroEstado = estado)
    }

    fun filtrarPorFecha(filtro: FiltroFecha) {
        _uiState.value = _uiState.value.copy(filtroFecha = filtro)
    }

    fun obtenerVentasFiltradas(): List<Venta> {
        val state = _uiState.value
        return state.ventas.filter { venta ->
            // Filtro por búsqueda
            val coincideBusqueda = state.searchQuery.isEmpty() ||
                venta.cliente.contains(state.searchQuery, ignoreCase = true) ||
                venta.id.contains(state.searchQuery, ignoreCase = true)

            // Filtro por estado
            val coincideEstado = state.filtroEstado == null || venta.estado == state.filtroEstado

            // Filtro por fecha (implementación básica)
            val coincideFecha = when (state.filtroFecha) {
                FiltroFecha.TODOS -> true
                FiltroFecha.HOY -> venta.fecha.contains("2024-01-15") // Simplificado para demo
                FiltroFecha.SEMANA -> true // Implementar lógica de semana
                FiltroFecha.MES -> true // Implementar lógica de mes
            }

            coincideBusqueda && coincideEstado && coincideFecha
        }
    }

    // Funciones para Nueva Venta
    fun cargarProductosParaVenta() {
        viewModelScope.launch {
            _nuevaVentaState.value = _nuevaVentaState.value.copy(isLoading = true)

            try {
                // Intentar obtener productos SOLO del servidor
                ventasRepository.obtenerProductosParaVenta().collect { result ->
                    result.fold(
                        onSuccess = { productosServidor ->
                            println("✅ VENTAS: Productos cargados desde servidor: ${productosServidor.size}")
                            val productosUI = productosServidor.map { producto -> producto.toUI() }
                            _nuevaVentaState.value = _nuevaVentaState.value.copy(
                                productosDisponibles = productosUI,
                                isLoading = false,
                                error = null
                            )
                        },
                        onFailure = { error ->
                            println("❌ VENTAS: Error al cargar productos: ${error.message}")
                            _nuevaVentaState.value = _nuevaVentaState.value.copy(
                                productosDisponibles = emptyList(),
                                isLoading = false,
                                error = "No se pudieron cargar los productos. Verifica tu conexión."
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                println("❌ VENTAS: Excepción al cargar productos: ${e.message}")
                _nuevaVentaState.value = _nuevaVentaState.value.copy(
                    productosDisponibles = emptyList(),
                    isLoading = false,
                    error = "Error de conexión. No se pudieron cargar los productos."
                )
            }
        }
    }

    fun actualizarCliente(cliente: String) {
        _nuevaVentaState.value = _nuevaVentaState.value.copy(cliente = cliente)
    }

    fun actualizarMetodoPago(metodo: MetodoPago) {
        _nuevaVentaState.value = _nuevaVentaState.value.copy(metodoPago = metodo)
    }

    fun buscarProductos(query: String) {
        _nuevaVentaState.value = _nuevaVentaState.value.copy(searchQuery = query)
    }

    fun actualizarObservaciones(texto: String) {
        _nuevaVentaState.value = _nuevaVentaState.value.copy(observaciones = texto)
    }

    fun iniciarNuevaVenta() {
        _nuevaVentaState.value = NuevaVentaUiState()
        cargarProductosParaVenta()
    }

    fun agregarProductoAlCarrito(producto: Producto) {
        val state = _nuevaVentaState.value
        val productoExistente = state.productos.find { it.id == producto.id }
        val productoUI = producto.toUI()
        val nuevosProductos = if (productoExistente != null) {
            // Incrementar cantidad si hay stock
            if (productoExistente.cantidad < productoUI.stock) {
                state.productos.map {
                    if (it.id == producto.id) {
                        it.copy(cantidad = it.cantidad + 1)
                    } else it
                }
            } else {
                state.productos // No cambiar si no hay stock
            }
        } else {
            // Agregar nuevo producto
            state.productos + ProductoCarrito(
                id = productoUI.id,
                nombre = productoUI.nombre,
                precio = productoUI.precio,
                cantidad = 1,
                stock = productoUI.stock,
                descripcion = productoUI.descripcion
            )
        }

        _nuevaVentaState.value = state.copy(productos = nuevosProductos)
    }

    fun actualizarCantidadProducto(id: Int, cantidad: Int) {
        val state = _nuevaVentaState.value
        val nuevosProductos = if (cantidad <= 0) {
            state.productos.filter { it.id != id }
        } else {
            state.productos.map {
                if (it.id == id && cantidad <= it.stock) {
                    it.copy(cantidad = cantidad)
                } else it
            }
        }
        _nuevaVentaState.value = state.copy(productos = nuevosProductos)
    }

    // Método que falta - alias para compatibilidad
    fun agregarProductoAVenta(producto: Producto) {
        val state = _nuevaVentaState.value
        val productoExistente = state.productos.find { it.id == producto.id }

        val nuevosProductos = if (productoExistente != null) {
            // Incrementar cantidad si hay stock
            if (productoExistente.cantidad < producto.toUI().stock) {
                state.productos.map {
                    if (it.id == producto.id) {
                        it.copy(cantidad = it.cantidad + 1)
                    } else it
                }
            } else {
                state.productos // No cambiar si no hay stock
            }
        } else {
            // Agregar nuevo producto
            state.productos + ProductoCarrito(
                id = producto.id,
                nombre = producto.nombre,
                precio = producto.precio,
                cantidad = 1,
                stock = producto.toUI().stock,
                descripcion = producto.descripcion
            )
        }

        _nuevaVentaState.value = state.copy(productos = nuevosProductos)
    }

    // Método que falta - alias para compatibilidad
    fun removerProductoDeVenta(id: Int) {
        removerProductoDelCarrito(id)
    }

    fun removerProductoDelCarrito(id: Int) {
        val state = _nuevaVentaState.value
        _nuevaVentaState.value = state.copy(
            productos = state.productos.filter { it.id != id }
        )
    }

    fun crearVenta() {
        val state = _nuevaVentaState.value
        val productosRequest = state.productos.map {
            ProductoVentaRequest(
                id = it.id,
                cantidad = it.cantidad
            )
        }
        // Usar NuevaVentaRequest y asegurar que metodoPago no sea nulo
        val ventaRequest = NuevaVentaRequest(
            cliente = state.cliente,
            productos = productosRequest,
            metodoPago = state.metodoPago ?: MetodoPago.EFECTIVO, // Valor por defecto si es nulo
            observaciones = state.observaciones
        )
        viewModelScope.launch {
            _nuevaVentaState.value = state.copy(isLoading = true, error = null)
            try {
                val result = ventasRepository.crearVenta(ventaRequest)
                result.fold(
                    onSuccess = { venta ->
                        _nuevaVentaState.value = state.copy(isLoading = false, ventaCreada = venta, error = null)
                        cargarDatos() // Actualiza la lista de ventas
                    },
                    onFailure = { error ->
                        _nuevaVentaState.value = state.copy(isLoading = false, error = error.message)
                    }
                )
            } catch (e: Exception) {
                _nuevaVentaState.value = state.copy(isLoading = false, error = e.message)
            }
        }
    }

    // Método faltante para DetalleVentaScreen
    fun actualizarEstadoVenta(ventaId: String, nuevoEstado: EstadoVenta) {
        viewModelScope.launch {
            try {
                ventasRepository.actualizarEstadoVenta(ventaId, nuevoEstado).fold(
                    onSuccess = { ventaActualizada ->
                        val ventasActualizadas = _uiState.value.ventas.map { venta ->
                            if (venta.id == ventaId) ventaActualizada else venta
                        }
                        _uiState.value = _uiState.value.copy(
                            ventas = ventasActualizadas,
                            successMessage = "Venta ${ventaActualizada.id} marcada como ${nuevoEstado.name.lowercase()}",
                            error = null
                        )
                    },
                    onFailure = { error ->
                        println("Error al actualizar estado de venta: ${error.message}")
                        _uiState.value = _uiState.value.copy(
                            error = "Error al actualizar estado: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                println("Excepción al actualizar estado de venta: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    error = "Error de conexi��n al actualizar estado"
                )
            }
        }
    }

    fun limpiarMensajeEstado() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun limpiarVentaCreada() {
        _nuevaVentaState.value = _nuevaVentaState.value.copy(ventaCreada = null, isLoading = false)
    }
}








