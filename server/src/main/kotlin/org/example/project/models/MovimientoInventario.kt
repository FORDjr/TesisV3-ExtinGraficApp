package org.example.project.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object MovimientosInventario : IntIdTable("movimientos_inventario") {
    val productoId = reference("producto_id", Productos)
    val tipo = enumerationByName("tipo", 20, TipoMovimientoInventario::class)
    val cantidad = integer("cantidad")
    val motivo = varchar("motivo", 120).nullable()
    val documento = varchar("documento", 120).nullable()
    val proveedorId = reference("proveedor_id", Proveedores).nullable()
    val usuarioId = reference("usuario_id", Usuarios).nullable()
    val observaciones = text("observaciones").nullable()
    val fechaRegistro = datetime("fecha_registro")
    val estadoAprobacion = enumerationByName("estado_aprobacion", 20, EstadoAprobacionMovimiento::class)
        .default(EstadoAprobacionMovimiento.APROBADO)
    val requiereAprobacion = bool("requiere_aprobacion").default(false)
    val aprobadoPor = reference("aprobado_por", Usuarios).nullable()
    val fechaAprobacion = datetime("fecha_aprobacion").nullable()
    val idempotenciaKey = varchar("idempotencia_key", 80).nullable().uniqueIndex()
}

class MovimientoInventario(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<MovimientoInventario>(MovimientosInventario)

    var producto by Producto referencedOn MovimientosInventario.productoId
    var tipo by MovimientosInventario.tipo
    var cantidad by MovimientosInventario.cantidad
    var motivo by MovimientosInventario.motivo
    var documento by MovimientosInventario.documento
    var proveedor by Proveedor optionalReferencedOn MovimientosInventario.proveedorId
    var usuario by Usuario optionalReferencedOn MovimientosInventario.usuarioId
    var observaciones by MovimientosInventario.observaciones
    var fechaRegistro by MovimientosInventario.fechaRegistro
    var estadoAprobacion by MovimientosInventario.estadoAprobacion
    var requiereAprobacion by MovimientosInventario.requiereAprobacion
    var aprobadoPor by Usuario optionalReferencedOn MovimientosInventario.aprobadoPor
    var fechaAprobacion by MovimientosInventario.fechaAprobacion
    var idempotenciaKey by MovimientosInventario.idempotenciaKey
}

enum class TipoMovimientoInventario { ENTRADA, SALIDA, AJUSTE }

enum class EstadoAprobacionMovimiento { APROBADO, PENDIENTE, RECHAZADO }

@Serializable
data class MovimientoInventarioResponse(
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
    val requiereAprobacion: Boolean,
    val aprobadoPor: Int? = null,
    val fechaAprobacion: String? = null,
    val idempotenciaKey: String? = null
)

@Serializable
data class CrearMovimientoInventarioRequest(
    val productoId: Int,
    val tipo: TipoMovimientoInventario,
    val cantidad: Int,
    val motivo: String? = null,
    val documento: String? = null,
    val proveedorId: Int? = null,
    val usuarioId: Int? = null,
    val observaciones: String? = null,
    val fechaRegistro: String? = null,
    val requiereAprobacion: Boolean? = null,
    val idempotenciaKey: String? = null
)

@Serializable
data class ActualizarMovimientoInventarioRequest(
    val motivo: String? = null,
    val documento: String? = null,
    val proveedorId: Int? = null,
    val usuarioId: Int? = null,
    val observaciones: String? = null
)

@Serializable
data class MovimientosPageResponse(
    val items: List<MovimientoInventarioResponse>,
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
    val movimientos: List<MovimientoInventarioResponse>,
    val totalEntradas: Int,
    val totalSalidas: Int,
    val totalAjustes: Int,
    val pendientes: Int,
    val saldoCalculado: Int
)

@Serializable
data class AprobarMovimientoRequest(
    val aprobado: Boolean = true,
    val usuarioId: Int? = null,
    val observaciones: String? = null
)
