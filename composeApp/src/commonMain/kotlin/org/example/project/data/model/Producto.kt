package org.example.project.data.model

import kotlinx.serialization.Serializable

// Modelo que coincide exactamente con tu API REST
@Serializable
data class Producto(
    val id: Int,
    val nombre: String,
    val descripcion: String? = null,
    val precio: Double,
    val cantidad: Int,
    val categoria: String,
    val fechaCreacion: String,
    val fechaActualizacion: String
)

// Modelo para el request (crear/actualizar producto)
@Serializable
data class ProductoRequest(
    val nombre: String,
    val descripcion: String? = null,
    val precio: Double,
    val cantidad: Int,
    val categoria: String
)

// Estados para la UI
data class ProductoUI(
    val id: Int,
    val nombre: String,
    val categoria: String,
    val stock: Int,
    val stockMinimo: Int = 5, // Valor por defecto
    val precio: Double,
    val descripcion: String? = null,
    val fechaIngreso: String,
    val estado: ProductoEstado = ProductoEstado.ACTIVO
) {
    val esBajoStock: Boolean get() = stock <= stockMinimo
    val precioFormateado: String get() = "$${String.format("%.2f", precio)}"
}

enum class ProductoEstado {
    ACTIVO, INACTIVO, AGOTADO
}

// Extensiones para convertir entre modelos
fun Producto.toUI(): ProductoUI = ProductoUI(
    id = id,
    nombre = nombre,
    categoria = categoria,
    stock = cantidad,
    precio = precio,
    descripcion = descripcion,
    fechaIngreso = fechaCreacion.substring(0, 10), // Solo la fecha
    estado = if (cantidad > 0) ProductoEstado.ACTIVO else ProductoEstado.AGOTADO
)

fun ProductoUI.toRequest(): ProductoRequest = ProductoRequest(
    nombre = nombre,
    descripcion = descripcion,
    precio = precio,
    cantidad = stock,
    categoria = categoria
)
