package org.example.project.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.api.ExportLinks
import org.example.project.data.model.EstadoAprobacionMovimiento
import org.example.project.data.model.KardexFilters
import org.example.project.data.model.ProductoUI
import org.example.project.data.model.TipoMovimientoInventario
import org.example.project.data.model.CrearMovimientoRequest
import org.example.project.data.repository.KardexRepository

class KardexViewModel(
    private val repository: KardexRepository = KardexRepository()
) : ViewModel() {

    val kardex = repository.kardex
    val isLoading = repository.isLoading
    val error = repository.error
    val productos = repository.productos
    val exportLinks = repository.exportLinks

    private val _filtros = MutableStateFlow(KardexFilters())
    val filtros: StateFlow<KardexFilters> = _filtros.asStateFlow()

    private val _productoBusqueda = MutableStateFlow("")
    val productoBusqueda: StateFlow<String> = _productoBusqueda.asStateFlow()

    private val _productoSeleccionado = MutableStateFlow<ProductoUI?>(null)
    val productoSeleccionado: StateFlow<ProductoUI?> = _productoSeleccionado.asStateFlow()

    init {
        viewModelScope.launch { repository.buscarProductos("") }
    }

    fun actualizarBusquedaProducto(texto: String) {
        _productoBusqueda.value = texto
        viewModelScope.launch {
            repository.buscarProductos(texto)
        }
    }

    fun seleccionarProducto(producto: ProductoUI?) {
        _productoSeleccionado.value = producto
        _filtros.value = _filtros.value.copy(productoId = producto?.id)
    }

    fun actualizarProductoIdManual(idTexto: String) {
        val id = idTexto.toIntOrNull()
        _productoSeleccionado.value = null
        _filtros.value = _filtros.value.copy(productoId = id)
    }

    fun updateTipo(tipo: TipoMovimientoInventario?) {
        _filtros.value = _filtros.value.copy(tipo = tipo)
    }

    fun updateEstado(estado: EstadoAprobacionMovimiento?) {
        _filtros.value = _filtros.value.copy(estado = estado)
    }

    fun updateDesde(desde: String) {
        _filtros.value = _filtros.value.copy(desde = desde.ifBlank { null })
    }

    fun updateHasta(hasta: String) {
        _filtros.value = _filtros.value.copy(hasta = hasta.ifBlank { null })
    }

    fun cargarKardex() {
        viewModelScope.launch {
            repository.cargarKardex(_filtros.value)
        }
    }

    fun aprobarMovimiento(id: Int, aprobado: Boolean, observaciones: String? = null) {
        viewModelScope.launch {
            repository.aprobarMovimiento(id, aprobado, observaciones)
        }
    }

    fun generarEnlacesExport(onReady: (ExportLinks) -> Unit = {}) {
        viewModelScope.launch {
            repository.refrescarExportLinks(_filtros.value)?.let { onReady(it) }
        }
    }

    fun crearAjustePendiente(
        cantidad: Int,
        motivo: String?,
        observaciones: String?,
        onResult: (Boolean) -> Unit = {}
    ) {
        val productoId = _filtros.value.productoId
        if (productoId == null || cantidad == 0) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            repository.crearAjustePendiente(
                CrearMovimientoRequest(
                    productoId = productoId,
                    tipo = TipoMovimientoInventario.AJUSTE,
                    cantidad = cantidad,
                    motivo = motivo?.ifBlank { null },
                    observaciones = observaciones?.ifBlank { null },
                    requiereAprobacion = true
                )
            )
            repository.cargarKardex(_filtros.value)
            onResult(true)
        }
    }

    fun limpiarError() = repository.limpiarError()

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}
