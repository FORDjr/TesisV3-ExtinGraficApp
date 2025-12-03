package org.example.project.data.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToLong

// Modelo que coincide exactamente con tu API REST
@Serializable
data class Producto(
    val id: Int,
    val nombre: String,
    val codigo: String = "",
    val descripcion: String? = null,
    val precio: Double,
    val precioCompra: Double = 0.0,
    val cantidad: Int,
    val categoria: String,
    val estado: EstadoProductoRemote = EstadoProductoRemote.ACTIVO,
    val proveedorId: Int? = null,
    val stockMinimo: Int = 0,
    val fechaCreacion: String,
    val fechaActualizacion: String
)

// Modelo para el request (crear/actualizar producto)
@Serializable
data class ProductoRequest(
    val nombre: String,
    val codigo: String? = null,
    val descripcion: String? = null,
    val precio: Long,
    val precioCompra: Long? = null,
    val cantidad: Int,
    val categoria: String,
    val estado: EstadoProductoRemote = EstadoProductoRemote.ACTIVO,
    val proveedorId: Int? = null,
    val stockMinimo: Int = 0
)

// Estados para la UI
data class ProductoUI(
    val id: Int,
    val nombre: String,
    val codigo: String = "",
    val categoria: String,
    val stock: Int,
    val stockMinimo: Int = 5, // Valor por defecto
    val precio: Double,
    val precioCompra: Double = 0.0,
    val descripcion: String? = null,
    val fechaIngreso: String,
    val estado: ProductoEstado = ProductoEstado.ACTIVO,
    val proveedorId: Int? = null
) {
    val esBajoStock: Boolean get() = stock <= stockMinimo
    val precioFormateado: String
        get() {
            val cents = (precio * 100).toInt()
            val entero = cents / 100
            val dec = cents % 100
            return "$" + entero.toString() + "." + dec.toString().padStart(2, '0')
        }
}

enum class ProductoEstado {
    ACTIVO, INACTIVO, AGOTADO
}

@Serializable
enum class EstadoProductoRemote { ACTIVO, INACTIVO }

// Extensiones para convertir entre modelos
fun Producto.toUI(): ProductoUI = ProductoUI(
    id = id,
    nombre = nombre,
    codigo = codigo,
    categoria = categoria,
    stock = cantidad,
    stockMinimo = stockMinimo.takeIf { it >= 0 } ?: 0, // usar backend
    precio = precio,
    precioCompra = precioCompra,
    descripcion = descripcion,
    fechaIngreso = fechaCreacion.substring(0, 10), // Solo la fecha
    estado = when {
        cantidad == 0 -> ProductoEstado.AGOTADO
        estado == EstadoProductoRemote.INACTIVO -> ProductoEstado.INACTIVO
        else -> ProductoEstado.ACTIVO
    },
    proveedorId = proveedorId
)

fun ProductoUI.toRequest(): ProductoRequest = ProductoRequest(
    nombre = nombre,
    codigo = codigo.ifBlank { null },
    descripcion = descripcion,
    precio = precio.roundToLong(),
    precioCompra = precioCompra.roundToLong(),
    cantidad = stock,
    categoria = categoria,
    estado = when (estado) {
        ProductoEstado.INACTIVO -> EstadoProductoRemote.INACTIVO
        else -> EstadoProductoRemote.ACTIVO
    },
    proveedorId = proveedorId,
    stockMinimo = stockMinimo
)

data class ProductoCarrito(
    val id: String,
    val nombre: String,
    val precio: Double,
    val cantidad: Int,
    val stock: Int,
    val subtotal: Double
)

@Serializable
data class InventarioPage(
    val items: List<Producto>,
    val total: Long,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)

data class InventarioQuery(
    val search: String = "",
    val categoria: String? = null,
    val estado: EstadoProductoRemote? = EstadoProductoRemote.ACTIVO
)
