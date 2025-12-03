package org.example.project.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import kotlinx.datetime.LocalDateTime

// Tabla de Ventas
object Ventas : IntIdTable("ventas") {
    val numero = varchar("numero", 30).uniqueIndex()
    val cliente = varchar("cliente", 255)
    val fecha = datetime("fecha")
    val total = long("total")
    val descuento = long("descuento").default(0L)
    val estado = enumerationByName("estado", 50, EstadoVenta::class)
    val metodoPago = enumerationByName("metodo_pago", 50, MetodoPago::class)
    val observaciones = text("observaciones").nullable()
    val vendedorId = reference("vendedor_id", Usuarios).nullable()
    val fechaCreacion = datetime("fecha_creacion")
    val fechaActualizacion = datetime("fecha_actualizacion")
}

// Tabla de productos en cada venta (relación muchos a muchos)
object VentaProductos : IntIdTable("venta_productos") {
    val ventaId = reference("venta_id", Ventas)
    val productoId = reference("producto_id", Productos)
    val cantidad = integer("cantidad")
    val precio = long("precio")
    val subtotal = long("subtotal")
}

// Entidad Venta
class Venta(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Venta>(Ventas)

    var numero by Ventas.numero
    var cliente by Ventas.cliente
    var fecha by Ventas.fecha
    var total by Ventas.total
    var descuento by Ventas.descuento
    var estado by Ventas.estado
    var metodoPago by Ventas.metodoPago
    var observaciones by Ventas.observaciones
    var vendedor by Usuario optionalReferencedOn Ventas.vendedorId
    var fechaCreacion by Ventas.fechaCreacion
    var fechaActualizacion by Ventas.fechaActualizacion
}

// Entidad VentaProducto
class VentaProducto(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<VentaProducto>(VentaProductos)

    var ventaId by VentaProductos.ventaId
    var productoId by VentaProductos.productoId
    var cantidad by VentaProductos.cantidad
    var precio by VentaProductos.precio
    var subtotal by VentaProductos.subtotal
}

// Enums
enum class EstadoVenta {
    PENDIENTE,
    COMPLETADA,
    CANCELADA
}

enum class MetodoPago {
    EFECTIVO,
    TARJETA,
    TRANSFERENCIA,
    CREDITO
}

// DTOs para las respuestas y requests
@Serializable
data class VentaRequest(
    val cliente: String,
    val productos: List<ProductoVentaRequest>,
    val metodoPago: MetodoPago,
    val vendedorId: Int? = null,
    val descuento: Long? = null,
    val observaciones: String? = null
)

@Serializable
data class ProductoVentaRequest(
    val id: Int,
    val cantidad: Int
)

@Serializable
data class VentaResponse(
    val id: String,
    val numero: String,
    val cliente: String,
    val fecha: String,
    val total: Long,
    val descuento: Long,
    val estado: EstadoVenta,
    val metodoPago: MetodoPago,
    val vendedorId: Int? = null,
    val observaciones: String? = null,
    val productos: List<ProductoVentaResponse> = emptyList()
)

@Serializable
data class ProductoVentaResponse(
    val id: Int,
    val nombre: String,
    val cantidad: Int,
    val precio: Long,
    val subtotal: Long
)

@Serializable
data class VentasListResponse(
    val ventas: List<VentaResponse>,
    val metricas: MetricasVentasResponse
)

@Serializable
data class MetricasVentasResponse(
    val ventasHoy: Long = 0L,
    val ordenesHoy: Int = 0,
    val ticketPromedio: Long = 0L,
    val ventasMes: Long = 0L,
    val crecimientoVentasHoy: Int = 0,
    val crecimientoOrdenes: Int = 0,
    val crecimientoTicket: Int = 0,
    val crecimientoMes: Int = 0
)

@Serializable
data class ActualizarEstadoRequest(
    val estado: EstadoVenta
)
