package org.example.project.services

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.project.models.*
import com.lowagie.text.Document
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class MovimientosInventarioService {

    data class Filtros(
        val productoId: Int? = null,
        val tipo: TipoMovimientoInventario? = null,
        val desde: LocalDateTime? = null,
        val hasta: LocalDateTime? = null,
        val estado: EstadoAprobacionMovimiento? = null
    )

    fun crearMovimiento(request: CrearMovimientoInventarioRequest): MovimientoInventarioResponse = transaction {
        val key = request.idempotenciaKey?.takeIf { it.isNotBlank() }
        if (key != null) {
            val existente = MovimientoInventario.find { MovimientosInventario.idempotenciaKey eq key }.firstOrNull()
            if (existente != null) return@transaction existente.toResponse()
        }

        val producto = Producto.findById(request.productoId) ?: error("Producto no encontrado")
        val ahora = request.fechaRegistro?.let { LocalDateTime.parse(it) }
            ?: Clock.System.now().toLocalDateTime(TimeZone.UTC)

        if (request.tipo != TipoMovimientoInventario.AJUSTE && request.cantidad <= 0) {
            error("La cantidad debe ser mayor a 0")
        }
        if (request.tipo == TipoMovimientoInventario.AJUSTE && request.cantidad == 0) {
            error("La cantidad de ajuste no puede ser 0")
        }

        val requiereAprobacion = request.requiereAprobacion == true && request.tipo == TipoMovimientoInventario.AJUSTE
        if (request.requiereAprobacion == true && request.tipo != TipoMovimientoInventario.AJUSTE) {
            error("Solo los ajustes pueden quedar pendientes de aprobación")
        }
        val delta = when (request.tipo) {
            TipoMovimientoInventario.ENTRADA -> request.cantidad
            TipoMovimientoInventario.SALIDA -> -request.cantidad
            TipoMovimientoInventario.AJUSTE -> request.cantidad
        }

        if (!requiereAprobacion) {
            val nuevoStock = producto.cantidad + delta
            if (nuevoStock < 0) error("Stock insuficiente para ${producto.nombre}")
            producto.cantidad = nuevoStock
            producto.fechaActualizacion = ahora
        }

        val movimiento = MovimientoInventario.new {
            this.producto = producto
            this.tipo = request.tipo
            this.cantidad = request.cantidad
            this.motivo = request.motivo
            this.documento = request.documento
            this.proveedor = request.proveedorId?.let { Proveedor.findById(it) }
            this.usuario = request.usuarioId?.let { Usuario.findById(it) }
            this.observaciones = request.observaciones
            this.fechaRegistro = ahora
            this.estadoAprobacion = if (requiereAprobacion) EstadoAprobacionMovimiento.PENDIENTE else EstadoAprobacionMovimiento.APROBADO
            this.requiereAprobacion = requiereAprobacion
            this.idempotenciaKey = key
        }

        movimiento.toResponse()
    }

    fun listarMovimientos(
        filtros: Filtros = Filtros(),
        limit: Int = 50,
        offset: Int = 0
    ): MovimientosPageResponse = transaction {
        var query = MovimientosInventario.selectAll()
        filtros.productoId?.let { query = query.andWhere { MovimientosInventario.productoId eq it } }
        filtros.tipo?.let { query = query.andWhere { MovimientosInventario.tipo eq it } }
        filtros.desde?.let { query = query.andWhere { MovimientosInventario.fechaRegistro greaterEq it } }
        filtros.hasta?.let { query = query.andWhere { MovimientosInventario.fechaRegistro lessEq it } }
        filtros.estado?.let { query = query.andWhere { MovimientosInventario.estadoAprobacion eq it } }

        val total = query.count()
        val movimientos = MovimientoInventario.wrapRows(
            query.orderBy(MovimientosInventario.fechaRegistro to SortOrder.DESC)
                .limit(limit, offset.toLong())
        ).map { it.toResponse() }

        MovimientosPageResponse(
            items = movimientos,
            total = total,
            limit = limit,
            offset = offset,
            hasMore = offset + movimientos.size < total
        )
    }

    fun obtenerMovimiento(id: Int): MovimientoInventarioResponse? = transaction {
        MovimientoInventario.findById(id)?.toResponse()
    }

    fun actualizarMovimiento(
        id: Int,
        request: ActualizarMovimientoInventarioRequest
    ): MovimientoInventarioResponse? = transaction {
        MovimientoInventario.findById(id)?.let { mov ->
            request.motivo?.let { mov.motivo = it }
            request.documento?.let { mov.documento = it }
            request.proveedorId?.let { mov.proveedor = Proveedor.findById(it) }
            request.usuarioId?.let { mov.usuario = Usuario.findById(it) }
            request.observaciones?.let { mov.observaciones = it }
            mov.toResponse()
        }
    }

    fun eliminarMovimiento(id: Int): Boolean = transaction {
        MovimientoInventario.findById(id)?.let { mov ->
            if (mov.estadoAprobacion == EstadoAprobacionMovimiento.APROBADO) {
                val delta = when (mov.tipo) {
                    TipoMovimientoInventario.ENTRADA -> -mov.cantidad
                    TipoMovimientoInventario.SALIDA -> mov.cantidad
                    TipoMovimientoInventario.AJUSTE -> -mov.cantidad
                }
                val nuevoStock = mov.producto.cantidad + delta
                if (nuevoStock < 0) error("Eliminar el movimiento dejaría stock negativo")
                mov.producto.cantidad = nuevoStock
                mov.producto.fechaActualizacion = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            }
            mov.delete()
            true
        } ?: false
    }

    fun resolverMovimiento(id: Int, aprobado: Boolean, usuarioId: Int?, observaciones: String?): MovimientoInventarioResponse = transaction {
        val movimiento = MovimientoInventario.findById(id) ?: error("Movimiento no encontrado")
        if (movimiento.estadoAprobacion != EstadoAprobacionMovimiento.PENDIENTE) {
            error("El movimiento ya fue resuelto")
        }
        val producto = movimiento.producto
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        if (aprobado) {
            val delta = when (movimiento.tipo) {
                TipoMovimientoInventario.ENTRADA -> movimiento.cantidad
                TipoMovimientoInventario.SALIDA -> -movimiento.cantidad
                TipoMovimientoInventario.AJUSTE -> movimiento.cantidad
            }
            val nuevoStock = producto.cantidad + delta
            if (nuevoStock < 0) error("Stock insuficiente para ${producto.nombre}")
            producto.cantidad = nuevoStock
            producto.fechaActualizacion = ahora
            movimiento.estadoAprobacion = EstadoAprobacionMovimiento.APROBADO
        } else {
            movimiento.estadoAprobacion = EstadoAprobacionMovimiento.RECHAZADO
        }
        movimiento.aprobadoPor = usuarioId?.let { Usuario.findById(it) }
        movimiento.fechaAprobacion = ahora
        observaciones?.let { movimiento.observaciones = (movimiento.observaciones?.plus("\n") ?: "") + it }
        movimiento.toResponse()
    }

    fun obtenerKardex(
        productoId: Int,
        tipo: TipoMovimientoInventario?,
        desde: LocalDateTime?,
        hasta: LocalDateTime?,
        estado: EstadoAprobacionMovimiento? = null
    ): KardexResponse = transaction {
        val producto = Producto.findById(productoId) ?: error("Producto no encontrado")
        var query = MovimientosInventario.selectAll().andWhere { MovimientosInventario.productoId eq productoId }
        tipo?.let { query = query.andWhere { MovimientosInventario.tipo eq it } }
        desde?.let { query = query.andWhere { MovimientosInventario.fechaRegistro greaterEq it } }
        hasta?.let { query = query.andWhere { MovimientosInventario.fechaRegistro lessEq it } }
        estado?.let { query = query.andWhere { MovimientosInventario.estadoAprobacion eq it } }

        val movimientos = MovimientoInventario.wrapRows(
            query.orderBy(MovimientosInventario.fechaRegistro to SortOrder.DESC)
        ).map { it.toResponse() }

        val totalEntradas = movimientos.filter { it.tipo == TipoMovimientoInventario.ENTRADA && it.estadoAprobacion == EstadoAprobacionMovimiento.APROBADO }
            .sumOf { it.cantidad }
        val totalSalidas = movimientos.filter { it.tipo == TipoMovimientoInventario.SALIDA && it.estadoAprobacion == EstadoAprobacionMovimiento.APROBADO }
            .sumOf { it.cantidad }
        val totalAjustes = movimientos.filter { it.tipo == TipoMovimientoInventario.AJUSTE && it.estadoAprobacion == EstadoAprobacionMovimiento.APROBADO }
            .sumOf { it.cantidad }
        val pendientes = movimientos.count { it.estadoAprobacion == EstadoAprobacionMovimiento.PENDIENTE }
        val saldoCalculado = producto.cantidad
        KardexResponse(
            producto = KardexProductoSummary(
                id = producto.id.value,
                nombre = producto.nombre,
                codigo = producto.codigo,
                categoria = producto.categoria,
                stockActual = producto.cantidad
            ),
            movimientos = movimientos,
            totalEntradas = totalEntradas,
            totalSalidas = totalSalidas,
            totalAjustes = totalAjustes,
            pendientes = pendientes,
            saldoCalculado = saldoCalculado
        )
    }

    fun exportarCsv(filtros: Filtros): String {
        val header = "ID,Producto,Tipo,Cantidad,Motivo,Documento,Proveedor,Usuario,Estado,AprobadoPor,Fecha,Idempotencia\n"
        val rows = listarMovimientos(filtros, limit = 5000, offset = 0).items.joinToString("\n") { mov ->
            listOf(
                mov.id,
                mov.productoId,
                mov.tipo,
                mov.cantidad,
                mov.motivo ?: "",
                mov.documento ?: "",
                mov.proveedorId ?: "",
                mov.usuarioId ?: "",
                mov.estadoAprobacion,
                mov.aprobadoPor ?: "",
                mov.fechaRegistro,
                mov.idempotenciaKey ?: ""
            ).joinToString(",") { value ->
                val text = value.toString().replace("\"", "\"\"")
                "\"$text\""
            }
        }
        return header + rows
    }

    fun exportarPdf(filtros: Filtros): ByteArray {
        val movimientos = listarMovimientos(filtros, limit = 5000, offset = 0).items
        val output = ByteArrayOutputStream()
        val document = Document(PageSize.A4.rotate())
        PdfWriter.getInstance(document, output)
        document.open()
        document.add(Paragraph("Kardex / Movimientos de inventario"))
        document.add(Paragraph("Generado: ${Clock.System.now().toLocalDateTime(TimeZone.UTC)}"))
        document.add(Paragraph("Total movimientos: ${movimientos.size}"))

        val table = PdfPTable(9)
        val headers = listOf("ID", "Producto", "Tipo", "Cantidad", "Motivo", "Doc", "Estado", "Aprobado por", "Fecha")
        headers.forEach { table.addCell(it) }
        movimientos.forEach { mov ->
            table.addCell(mov.id.toString())
            table.addCell(mov.productoId.toString())
            table.addCell(mov.tipo.name)
            table.addCell(mov.cantidad.toString())
            table.addCell(mov.motivo ?: "-")
            table.addCell(mov.documento ?: "-")
            table.addCell(mov.estadoAprobacion.name)
            table.addCell(mov.aprobadoPor?.toString() ?: "-")
            table.addCell(mov.fechaRegistro)
        }

        document.add(table)
        document.close()
        return output.toByteArray()
    }

    private fun MovimientoInventario.toResponse() = MovimientoInventarioResponse(
        id = id.value,
        productoId = producto.id.value,
        tipo = tipo,
        cantidad = cantidad,
        motivo = motivo,
        documento = documento,
        proveedorId = proveedor?.id?.value,
        usuarioId = usuario?.id?.value,
        observaciones = observaciones,
        fechaRegistro = fechaRegistro.toString(),
        estadoAprobacion = estadoAprobacion,
        requiereAprobacion = requiereAprobacion,
        aprobadoPor = aprobadoPor?.id?.value,
        fechaAprobacion = fechaAprobacion?.toString(),
        idempotenciaKey = idempotenciaKey
    )
}
