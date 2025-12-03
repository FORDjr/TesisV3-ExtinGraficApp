package org.example.project.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.data.api.ExportLinks
import org.example.project.data.api.InventarioApiService
import org.example.project.data.api.MovimientosApiService
import org.example.project.data.model.CrearMovimientoRequest
import org.example.project.data.model.EstadoProductoRemote
import org.example.project.data.model.InventarioQuery
import org.example.project.data.model.KardexFilters
import org.example.project.data.model.KardexResponse
import org.example.project.data.model.ProductoUI
import org.example.project.data.model.toUI

class KardexRepository(
    private val movimientosApi: MovimientosApiService = MovimientosApiService(),
    private val inventarioApi: InventarioApiService = InventarioApiService()
) {
    private val _kardex = MutableStateFlow<KardexResponse?>(null)
    val kardex: StateFlow<KardexResponse?> = _kardex.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _productos = MutableStateFlow<List<ProductoUI>>(emptyList())
    val productos: StateFlow<List<ProductoUI>> = _productos.asStateFlow()

    private val _exportLinks = MutableStateFlow<ExportLinks?>(null)
    val exportLinks: StateFlow<ExportLinks?> = _exportLinks.asStateFlow()

    private var lastFilters: KardexFilters? = null

    suspend fun cargarKardex(filtros: KardexFilters) {
        if (filtros.productoId == null) {
            _error.value = "Selecciona un producto para ver su kardex"
            return
        }
        try {
            _isLoading.value = true
            _error.value = null
            lastFilters = filtros
            _kardex.value = movimientosApi.obtenerKardex(filtros)
        } catch (e: Exception) {
            _error.value = e.message ?: "Error al cargar kardex"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun buscarProductos(query: String) {
        try {
            _error.value = null
            val page = inventarioApi.obtenerProductos(
                InventarioQuery(search = query, estado = EstadoProductoRemote.ACTIVO),
                limit = 25,
                offset = 0
            )
            _productos.value = page.items.map { it.toUI() }
        } catch (_: Exception) {
            // No bloquear por error de sugerencias
        }
    }

    suspend fun aprobarMovimiento(id: Int, aprobado: Boolean, observaciones: String?) {
        try {
            _isLoading.value = true
            movimientosApi.aprobarMovimiento(id, aprobado, observaciones)
            lastFilters?.let { cargarKardex(it) }
        } catch (e: Exception) {
            _error.value = e.message ?: "No se pudo actualizar el movimiento"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun crearAjustePendiente(request: CrearMovimientoRequest) {
        try {
            _isLoading.value = true
            movimientosApi.crearMovimiento(request.copy(requiereAprobacion = true))
            lastFilters?.let { cargarKardex(it) }
        } catch (e: Exception) {
            _error.value = e.message ?: "No se pudo registrar el ajuste"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun refrescarExportLinks(filtros: KardexFilters): ExportLinks? {
        return try {
            movimientosApi.exportLinks(filtros).also { _exportLinks.value = it }
        } catch (e: Exception) {
            _error.value = e.message ?: "No se pudieron generar los enlaces de exportación"
            null
        }
    }

    fun limpiarError() {
        _error.value = null
    }

    fun close() {
        movimientosApi.close()
        inventarioApi.close()
    }
}
