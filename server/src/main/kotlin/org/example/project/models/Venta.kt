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
    val clienteRut = varchar("cliente_rut", 50).nullable()
    val clienteDireccion = varchar("cliente_direccion", 255).nullable()
    val clienteTelefono = varchar("cliente_telefono", 80).nullable()
    val clienteEmail = varchar("cliente_email", 120).nullable()
    val fecha = datetime("fecha")
    val subtotal = long("subtotal").default(0L)
    val impuestos = long("impuestos").default(0L)
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
    val descuento = long("descuento").default(0L)
    val iva = double("iva").default(0.0)
    val devuelto = integer("devuelto").default(0)
}

// Entidad Venta
class Venta(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Venta>(Ventas)

    var numero by Ventas.numero
    var cliente by Ventas.cliente
    var clienteRut by Ventas.clienteRut
    var clienteDireccion by Ventas.clienteDireccion
    var clienteTelefono by Ventas.clienteTelefono
    var clienteEmail by Ventas.clienteEmail
    var fecha by Ventas.fecha
    var subtotal by Ventas.subtotal
    var impuestos by Ventas.impuestos
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
    var descuento by VentaProductos.descuento
    var iva by VentaProductos.iva
    var devuelto by VentaProductos.devuelto
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
data class ClienteFormal(
    val nombre: String,
    val rut: String? = null,
    val direccion: String? = null,
    val telefono: String? = null,
    val email: String? = null
)

@Serializable
data class VentaRequest(
    val cliente: String,
    val clienteFormal: ClienteFormal? = null,
    val productos: List<ProductoVentaRequest>,
    val metodoPago: MetodoPago,
    val vendedorId: Int? = null,
    val descuento: Long? = null,
    val observaciones: String? = null
)

@Serializable
data class ProductoVentaRequest(
    val id: Int,
    val cantidad: Int,
    val precio: Long? = null,
    val descuento: Long? = null,
    val iva: Double? = null
)

@Serializable
data class VentaResponse(
    val id: String,
    val numero: String,
    val cliente: String,
    val clienteFormal: ClienteFormal? = null,
    val fecha: String,
    val subtotal: Long,
    val impuestos: Long,
    val total: Long,
    val descuento: Long,
    val estado: EstadoVenta,
    val metodoPago: MetodoPago,
    val vendedorId: Int? = null,
    val observaciones: String? = null,
    val totalDevuelto: Long = 0L,
    val saldo: Long = total - totalDevuelto,
    val productos: List<ProductoVentaResponse> = emptyList()
)

@Serializable
data class ProductoVentaResponse(
    val id: Int,
    val nombre: String,
    val cantidad: Int,
    val precio: Long,
    val subtotal: Long,
    val descuento: Long = 0L,
    val iva: Double = 0.0,
    val devuelto: Int = 0
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

@Serializable
data class DevolucionParcialRequest(
    val items: List<ItemDevolucionRequest>,
    val motivo: String? = null
)

@Serializable
data class ItemDevolucionRequest(
    val productoId: Int,
    val cantidad: Int
)
