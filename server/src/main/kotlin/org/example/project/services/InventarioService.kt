package org.example.project.services

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.project.models.*
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class InventarioService {

    data class Filtros(
        val search: String? = null,
        val codigo: String? = null,
        val categoria: String? = null,
        val estado: EstadoProducto? = null,
        val criticos: Boolean = false
    )

    private val movimientosService = MovimientosInventarioService()

    fun crearProducto(request: ProductoRequest): ProductoResponse = transaction {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        val producto = Producto.new {
            nombre = request.nombre
            codigo = request.codigo?.ifBlank { generarCodigo() } ?: generarCodigo()
            descripcion = request.descripcion
            precio = request.precio
            precioCompra = request.precioCompra ?: request.precio
            cantidad = request.cantidad
            categoria = request.categoria
            estado = request.estado ?: EstadoProducto.ACTIVO
            stockMinimo = request.stockMinimo
            proveedor = request.proveedorId?.let { Proveedor.findById(it) }
            fechaCreacion = ahora
            fechaActualizacion = ahora
        }

        producto.toResponse()
    }

    fun listarProductos(
        filtros: Filtros = Filtros(),
        limit: Int = 50,
        offset: Int = 0
    ): InventarioPageResponse = transaction {
        var query = Productos.selectAll()

        filtros.estado?.let { query = query.andWhere { Productos.estado eq it } }
        filtros.categoria?.takeIf { it.isNotBlank() }?.let { categoria ->
            val pattern = "%${categoria.trim().lowercase()}%"
            query = query.andWhere { Productos.categoria.lowerCase() like pattern }
        }
        filtros.codigo?.takeIf { it.isNotBlank() }?.let { codigo ->
            val pattern = "%${codigo.trim().lowercase()}%"
            query = query.andWhere { Productos.codigo.lowerCase() like pattern }
        }
        filtros.search?.takeIf { it.isNotBlank() }?.let { search ->
            val pattern = "%${search.trim().lowercase()}%"
            query = query.andWhere {
                (Productos.nombre.lowerCase() like pattern) or
                    (Productos.codigo.lowerCase() like pattern) or
                    (Productos.categoria.lowerCase() like pattern)
            }
        }
        if (filtros.criticos) {
            query = query.andWhere { Productos.cantidad lessEq Productos.stockMinimo }
        }

        val total = query.count()
        val productos = Producto.wrapRows(
            query.orderBy(Productos.fechaActualizacion to SortOrder.DESC)
                .limit(limit, offset.toLong())
        ).map { it.toResponse() }

        InventarioPageResponse(
            items = productos,
            total = total,
            limit = limit,
            offset = offset,
            hasMore = offset + productos.size < total
        )
    }

    fun obtenerTodosLosProductos(estado: EstadoProducto? = null): List<ProductoResponse> =
        listarProductos(filtros = Filtros(estado = estado), limit = 5000).items

    fun obtenerProductoPorId(id: Int): ProductoResponse? = transaction {
        Producto.findById(id)?.toResponse()
    }

    fun actualizarProducto(id: Int, request: ProductoRequest): ProductoResponse? = transaction {
        Producto.findById(id)?.let { producto ->
            val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val deltaCantidad = request.cantidad - producto.cantidad

            producto.nombre = request.nombre
            request.codigo?.takeIf { it.isNotBlank() }?.let { producto.codigo = it }
            producto.descripcion = request.descripcion
            producto.precio = request.precio
            producto.precioCompra = request.precioCompra ?: producto.precioCompra
            producto.categoria = request.categoria
            producto.estado = request.estado ?: producto.estado
            producto.proveedor = request.proveedorId?.let { Proveedor.findById(it) }
            producto.stockMinimo = request.stockMinimo
            producto.fechaActualizacion = ahora

            if (deltaCantidad != 0) {
                movimientosService.crearMovimiento(
                    CrearMovimientoInventarioRequest(
                        productoId = producto.id.value,
                        tipo = TipoMovimientoInventario.AJUSTE,
                        cantidad = deltaCantidad,
                        motivo = "Ajuste manual durante edición de producto",
                        proveedorId = request.proveedorId,
                        fechaRegistro = ahora.toString()
                    )
                )
            }

            producto.toResponse()
        }
    }

    fun eliminarProducto(id: Int): Boolean = transaction {
        Producto.findById(id)?.let { producto ->
            producto.estado = EstadoProducto.INACTIVO
            producto.fechaActualizacion = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            true
        } ?: false
    }

    fun actualizarStock(id: Int, nuevaCantidad: Int): ProductoResponse? = transaction {
        if (nuevaCantidad < 0) error("La cantidad no puede ser negativa")
        Producto.findById(id)?.let { producto ->
            if (nuevaCantidad < producto.cantidad) {
                error("El restock solo permite mantener o aumentar el stock actual")
            }
            val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            val delta = nuevaCantidad - producto.cantidad

            if (delta != 0) {
                movimientosService.crearMovimiento(
                    CrearMovimientoInventarioRequest(
                        productoId = producto.id.value,
                        tipo = TipoMovimientoInventario.ENTRADA,
                        cantidad = delta,
                        motivo = "Restock manual desde Inventario",
                        proveedorId = producto.proveedor?.id?.value,
                        fechaRegistro = ahora.toString()
                    )
                )
            } else {
                producto.fechaActualizacion = ahora
            }

            Producto.findById(id)?.toResponse()
        }
    }

    fun productosCriticos(): List<ProductoResponse> = transaction {
        Producto.all()
            .filter { it.estado == EstadoProducto.ACTIVO && it.cantidad <= it.stockMinimo }
            .map { it.toResponse() }
    }

    fun cambiarEstado(id: Int, estado: EstadoProducto): ProductoResponse? = transaction {
        Producto.findById(id)?.let { producto ->
            producto.estado = estado
            producto.fechaActualizacion = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            producto.toResponse()
        }
    }

    fun obtenerCategorias(onlyActive: Boolean = true): List<String> = transaction {
        val query = if (onlyActive) Producto.find { Productos.estado eq EstadoProducto.ACTIVO } else Producto.all()
        query.mapNotNull { it.categoria.takeIf { cat -> cat.isNotBlank() } }.distinct()
    }

    private fun Producto.toResponse(): ProductoResponse = ProductoResponse(
        id = id.value,
        nombre = nombre,
        codigo = codigo,
        descripcion = descripcion,
        precio = precio,
        precioCompra = precioCompra,
        cantidad = cantidad,
        categoria = categoria,
        estado = estado,
        proveedorId = proveedor?.id?.value,
        stockMinimo = stockMinimo,
        fechaCreacion = fechaCreacion.toString(),
        fechaActualizacion = fechaActualizacion.toString()
    )

    private fun generarCodigo(): String = "PRD-${System.currentTimeMillis()}"
}
