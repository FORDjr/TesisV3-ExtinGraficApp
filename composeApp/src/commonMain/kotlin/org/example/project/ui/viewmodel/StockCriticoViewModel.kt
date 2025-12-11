package org.example.project.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.data.api.MovimientosApiService
import org.example.project.data.model.CrearMovimientoRequest
import org.example.project.data.model.ProductoUI
import org.example.project.data.model.TipoMovimientoInventario
import org.example.project.data.repository.InventarioRepository

data class StockCriticoState(
    val loading: Boolean = true,
    val error: String? = null,
    val productos: List<ProductoUI> = emptyList(),
    val mensaje: String? = null
)

class StockCriticoViewModel(
    private val inventarioRepository: InventarioRepository = InventarioRepository(),
    private val movimientosApi: MovimientosApiService = MovimientosApiService()
) : ViewModel() {

    private val _state = MutableStateFlow(StockCriticoState())
    val state: StateFlow<StockCriticoState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                val productos = inventarioRepository.obtenerCriticos()
                _state.update { it.copy(loading = false, productos = productos) }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "No se pudo cargar stock critico") }
            }
        }
    }

    fun restock(producto: ProductoUI, incremento: Int, onFinish: (Boolean) -> Unit = {}) {
        if (incremento <= 0) {
            onFinish(false)
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, mensaje = null) }
            val nuevoStock = (producto.stock + incremento).coerceAtLeast(0)
            val ok = inventarioRepository.actualizarStock(producto.id, nuevoStock)
            if (ok) {
                _state.update { it.copy(mensaje = "Stock actualizado para ${producto.nombre}") }
                refresh()
            } else {
                _state.update { it.copy(loading = false, error = "No se pudo actualizar stock") }
            }
            onFinish(ok)
        }
    }

    fun ajustar(
        producto: ProductoUI,
        cantidad: Int,
        motivo: String?,
        observaciones: String?,
        onFinish: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, mensaje = null) }
            try {
                movimientosApi.crearMovimiento(
                    CrearMovimientoRequest(
                        productoId = producto.id,
                        tipo = TipoMovimientoInventario.AJUSTE,
                        cantidad = cantidad,
                        motivo = motivo?.ifBlank { null },
                        observaciones = observaciones?.ifBlank { null },
                        requiereAprobacion = true
                    )
                )
                _state.update { it.copy(mensaje = "Ajuste enviado para ${producto.nombre}") }
                refresh()
                onFinish(true)
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "No se pudo registrar el ajuste") }
                onFinish(false)
            }
        }
    }

    fun limpiarMensajes() {
        _state.update { it.copy(error = null, mensaje = null) }
    }

    override fun onCleared() {
        super.onCleared()
        inventarioRepository.close()
        movimientosApi.close()
    }
}
