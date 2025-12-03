package org.example.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MovimientoInventario(
    val id: Int,
    val productoId: Int,
    val tipo: TipoMovimientoInventario,
    val cantidad: Int,
    val motivo: String? = null,
    val documento: String? = null,
    val proveedorId: Int? = null,
    val usuarioId: Int? = null,
    val observaciones: String? = null,
    val fechaRegistro: String,
    val estadoAprobacion: EstadoAprobacionMovimiento,
    val requiereAprobacion: Boolean = false,
    val aprobadoPor: Int? = null,
    val fechaAprobacion: String? = null
)

@Serializable
data class MovimientosPage(
    val items: List<MovimientoInventario>,
    val total: Long,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)

@Serializable
data class KardexProductoSummary(
    val id: Int,
    val nombre: String,
    val codigo: String,
    val categoria: String,
    val stockActual: Int
)

@Serializable
data class KardexResponse(
    val producto: KardexProductoSummary,
    val movimientos: List<MovimientoInventario>,
    val totalEntradas: Int,
    val totalSalidas: Int,
    val totalAjustes: Int,
    val pendientes: Int,
    val saldoCalculado: Int
)

@Serializable
data class CrearMovimientoRequest(
    val productoId: Int,
    val tipo: TipoMovimientoInventario,
    val cantidad: Int,
    val motivo: String? = null,
    val documento: String? = null,
    val proveedorId: Int? = null,
    val usuarioId: Int? = null,
    val observaciones: String? = null,
    val fechaRegistro: String? = null,
    val requiereAprobacion: Boolean? = null
)

@Serializable
data class AprobarMovimientoPayload(
    val aprobado: Boolean = true,
    val usuarioId: Int? = null,
    val observaciones: String? = null
)

@Serializable
enum class TipoMovimientoInventario { ENTRADA, SALIDA, AJUSTE }

@Serializable
enum class EstadoAprobacionMovimiento { APROBADO, PENDIENTE, RECHAZADO }

data class KardexFilters(
    val productoId: Int? = null,
    val tipo: TipoMovimientoInventario? = null,
    val estado: EstadoAprobacionMovimiento? = null,
    val desde: String? = null,
    val hasta: String? = null
)
