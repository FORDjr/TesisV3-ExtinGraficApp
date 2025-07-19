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
    val cliente = varchar("cliente", 255)
    val fecha = datetime("fecha")
    val total = decimal("total", 10, 2)
    val estado = enumerationByName("estado", 50, EstadoVenta::class)
    val metodoPago = enumerationByName("metodo_pago", 50, MetodoPago::class)
    val observaciones = text("observaciones").nullable()
    val fechaCreacion = datetime("fecha_creacion")
    val fechaActualizacion = datetime("fecha_actualizacion")
}

// Tabla de productos en cada venta (relación muchos a muchos)
object VentaProductos : IntIdTable("venta_productos") {
    val ventaId = reference("venta_id", Ventas)
    val productoId = reference("producto_id", Productos)
    val cantidad = integer("cantidad")
    val precio = decimal("precio", 10, 2)
    val subtotal = decimal("subtotal", 10, 2)
}

// Entidad Venta
class Venta(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Venta>(Ventas)

    var cliente by Ventas.cliente
    var fecha by Ventas.fecha
    var total by Ventas.total
    var estado by Ventas.estado
    var metodoPago by Ventas.metodoPago
    var observaciones by Ventas.observaciones
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
    val observaciones: String? = null
)

@Serializable
data class ProductoVentaRequest(
    val id: Int,
    val cantidad: Int,
    val precio: Double
)

@Serializable
data class VentaResponse(
    val id: String,
    val cliente: String,
    val fecha: String,
    val total: Double,
    val estado: EstadoVenta,
    val metodoPago: MetodoPago,
    val observaciones: String? = null,
    val productos: List<ProductoVentaResponse> = emptyList()
)

@Serializable
data class ProductoVentaResponse(
    val id: Int,
    val nombre: String,
    val cantidad: Int,
    val precio: Double,
    val subtotal: Double
)

@Serializable
data class MetricasVentasResponse(
    val ventasHoy: Double = 0.0,
    val ordenesHoy: Int = 0,
    val ticketPromedio: Double = 0.0,
    val ventasMes: Double = 0.0,
    val crecimientoVentasHoy: Double = 0.0,
    val crecimientoOrdenes: Double = 0.0,
    val crecimientoTicket: Double = 0.0,
    val crecimientoMes: Double = 0.0
)

@Serializable
data class ActualizarEstadoRequest(
    val estado: EstadoVenta
)
