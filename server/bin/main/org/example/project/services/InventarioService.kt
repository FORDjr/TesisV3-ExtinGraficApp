package org.example.project.services

import org.jetbrains.exposed.sql.transactions.transaction
import org.example.project.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.math.BigDecimal

class InventarioService {

    fun crearProducto(request: ProductoRequest): ProductoResponse = transaction {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val producto = Producto.new {
            nombre = request.nombre
            descripcion = request.descripcion
            precio = BigDecimal.valueOf(request.precio)
            cantidad = request.cantidad
            categoria = request.categoria
            fechaCreacion = ahora
            fechaActualizacion = ahora
        }

        ProductoResponse(
            id = producto.id.value,
            nombre = producto.nombre,
            descripcion = producto.descripcion,
            precio = producto.precio.toDouble(),
            cantidad = producto.cantidad,
            categoria = producto.categoria,
            fechaCreacion = producto.fechaCreacion.toString(),
            fechaActualizacion = producto.fechaActualizacion.toString()
        )
    }

    fun obtenerTodosLosProductos(): List<ProductoResponse> = transaction {
        Producto.all().map { producto ->
            ProductoResponse(
                id = producto.id.value,
                nombre = producto.nombre,
                descripcion = producto.descripcion,
                precio = producto.precio.toDouble(),
                cantidad = producto.cantidad,
                categoria = producto.categoria,
                fechaCreacion = producto.fechaCreacion.toString(),
                fechaActualizacion = producto.fechaActualizacion.toString()
            )
        }
    }

    fun obtenerProductoPorId(id: Int): ProductoResponse? = transaction {
        Producto.findById(id)?.let { producto ->
            ProductoResponse(
                id = producto.id.value,
                nombre = producto.nombre,
                descripcion = producto.descripcion,
                precio = producto.precio.toDouble(),
                cantidad = producto.cantidad,
                categoria = producto.categoria,
                fechaCreacion = producto.fechaCreacion.toString(),
                fechaActualizacion = producto.fechaActualizacion.toString()
            )
        }
    }

    fun actualizarProducto(id: Int, request: ProductoRequest): ProductoResponse? = transaction {
        Producto.findById(id)?.let { producto ->
            val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)

            producto.nombre = request.nombre
            producto.descripcion = request.descripcion
            producto.precio = BigDecimal.valueOf(request.precio)
            producto.cantidad = request.cantidad
            producto.categoria = request.categoria
            producto.fechaActualizacion = ahora

            ProductoResponse(
                id = producto.id.value,
                nombre = producto.nombre,
                descripcion = producto.descripcion,
                precio = producto.precio.toDouble(),
                cantidad = producto.cantidad,
                categoria = producto.categoria,
                fechaCreacion = producto.fechaCreacion.toString(),
                fechaActualizacion = producto.fechaActualizacion.toString()
            )
        }
    }

    fun eliminarProducto(id: Int): Boolean = transaction {
        Producto.findById(id)?.let { producto ->
            producto.delete()
            true
        } ?: false
    }

    fun actualizarStock(id: Int, nuevaCantidad: Int): ProductoResponse? = transaction {
        Producto.findById(id)?.let { producto ->
            val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)

            producto.cantidad = nuevaCantidad
            producto.fechaActualizacion = ahora

            ProductoResponse(
                id = producto.id.value,
                nombre = producto.nombre,
                descripcion = producto.descripcion,
                precio = producto.precio.toDouble(),
                cantidad = producto.cantidad,
                categoria = producto.categoria,
                fechaCreacion = producto.fechaCreacion.toString(),
                fechaActualizacion = producto.fechaActualizacion.toString()
            )
        }
    }
}
