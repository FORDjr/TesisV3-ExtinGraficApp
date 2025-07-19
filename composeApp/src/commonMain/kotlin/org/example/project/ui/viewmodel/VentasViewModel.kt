package org.example.project.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.example.project.data.repository.VentasRepository
import org.example.project.data.models.*
import org.example.project.data.model.Producto
import org.example.project.data.model.toUI

class VentasViewModel(
    private val ventasRepository: VentasRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VentasUiState())
    val uiState: StateFlow<VentasUiState> = _uiState.asStateFlow()

    private val _nuevaVentaState = MutableStateFlow(NuevaVentaUiState())
    val nuevaVentaState: StateFlow<NuevaVentaUiState> = _nuevaVentaState.asStateFlow()

    // Nuevo estado para crear productos
    private val _nuevoProductoState = MutableStateFlow(NuevoProductoUiState())
    val nuevoProductoState: StateFlow<NuevoProductoUiState> = _nuevoProductoState.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                // Cargar ventas y métricas en paralelo
                val ventasFlow = ventasRepository.obtenerVentas()
                val metricasFlow = ventasRepository.obtenerMetricas()

                combine(ventasFlow, metricasFlow) { ventasResult, metricasResult ->
                    ventasResult.fold(
                        onSuccess = { ventas ->
                            metricasResult.fold(
                                onSuccess = { metricas ->
                                    _uiState.value = _uiState.value.copy(
                                        ventas = ventas,
                                        metricas = metricas,
                                        isLoading = false,
                                        error = null
                                    )
                                },
                                onFailure = { error ->
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        error = "Error al cargar métricas: ${error.message}",
                                        ventas = emptyList(),
                                        metricas = ventasRepository.obtenerMetricasEjemplo()
                                    )
                                }
                            )
                        },
                        onFailure = { error ->
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Error al cargar ventas: ${error.message}",
                                ventas = emptyList(),
                                metricas = ventasRepository.obtenerMetricasEjemplo()
                            )
                        }
                    )
                }.collect()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error inesperado: ${e.message}",
                    ventas = emptyList(),
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
        if (!state.isValid) return

        viewModelScope.launch {
            _nuevaVentaState.value = state.copy(isLoading = true, error = null)

            val nuevaVenta = NuevaVentaRequest(
                cliente = state.cliente,
                metodoPago = state.metodoPago!!,
                observaciones = state.observaciones.takeIf { it.isNotBlank() },
                productos = state.productos.map {
                    ProductoVentaRequest(
                        id = it.id,
                        cantidad = it.cantidad,
                        precio = it.precio
                    )
                }
            )

            try {
                ventasRepository.crearVenta(nuevaVenta).fold(
                    onSuccess = { ventaCreada ->
                        // Limpiar el formulario después de crear la venta
                        _nuevaVentaState.value = NuevaVentaUiState()
                        // Recargar las ventas para mostrar la nueva
                        cargarDatos()
                    },
                    onFailure = { error ->
                        _nuevaVentaState.value = state.copy(
                            isLoading = false,
                            error = "Error al crear la venta: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _nuevaVentaState.value = state.copy(
                    isLoading = false,
                    error = "Error de conexión: ${e.message}"
                )
            }
        }
    }

    // Método faltante para DetalleVentaScreen
    fun actualizarEstadoVenta(ventaId: String, nuevoEstado: EstadoVenta) {
        viewModelScope.launch {
            try {
                ventasRepository.actualizarEstadoVenta(ventaId, nuevoEstado).fold(
                    onSuccess = {
                        // Actualizar la venta en el estado local
                        val ventasActualizadas = _uiState.value.ventas.map { venta ->
                            if (venta.id == ventaId) {
                                venta.copy(estado = nuevoEstado)
                            } else venta
                        }
                        _uiState.value = _uiState.value.copy(ventas = ventasActualizadas)
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
}
