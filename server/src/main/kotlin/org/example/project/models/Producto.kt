package org.example.project.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

// Tabla de la base de datos
object Productos : IntIdTable() {
    val nombre = varchar("nombre", 100)
    val codigo = varchar("codigo", 80).uniqueIndex()
    val descripcion = text("descripcion").nullable()
    val precio = long("precio")
    val precioCompra = long("precio_compra").default(0L)
    val cantidad = integer("cantidad")
    val categoria = varchar("categoria", 50)
    val estado = enumerationByName("estado", 30, EstadoProducto::class).default(EstadoProducto.ACTIVO)
    val proveedorId = reference("proveedor_id", Proveedores).nullable()
    val stockMinimo = integer("stock_minimo").default(0)
    val fechaCreacion = datetime("fecha_creacion")
    val fechaActualizacion = datetime("fecha_actualizacion")
}

// Entidad DAO
class Producto(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Producto>(Productos)

    var nombre by Productos.nombre
    var codigo by Productos.codigo
    var descripcion by Productos.descripcion
    var precio by Productos.precio
    var precioCompra by Productos.precioCompra
    var cantidad by Productos.cantidad
    var categoria by Productos.categoria
    var estado by Productos.estado
    var proveedor by Proveedor optionalReferencedOn Productos.proveedorId
    var stockMinimo by Productos.stockMinimo
    var fechaCreacion by Productos.fechaCreacion
    var fechaActualizacion by Productos.fechaActualizacion
}

// DTOs para la API
@Serializable
data class ProductoRequest(
    val nombre: String,
    val codigo: String? = null,
    val descripcion: String? = null,
    val precio: Long,
    val precioCompra: Long? = null,
    val cantidad: Int,
    val categoria: String,
    val estado: EstadoProducto? = null,
    val proveedorId: Int? = null,
    val stockMinimo: Int = 0
)

@Serializable
data class ProductoResponse(
    val id: Int,
    val nombre: String,
    val codigo: String,
    val descripcion: String? = null,
    val precio: Long,
    val precioCompra: Long,
    val cantidad: Int,
    val categoria: String,
    val estado: EstadoProducto,
    val proveedorId: Int? = null,
    val stockMinimo: Int,
    val fechaCreacion: String,
    val fechaActualizacion: String
)

@Serializable
data class InventarioPageResponse(
    val items: List<ProductoResponse>,
    val total: Long,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)

@Serializable
data class ActualizarEstadoProductoRequest(val estado: EstadoProducto)

enum class EstadoProducto { ACTIVO, INACTIVO }
