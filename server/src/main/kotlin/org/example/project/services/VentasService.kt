package org.example.project.services

import com.lowagie.text.Document
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import java.io.ByteArrayOutputStream
import kotlin.math.roundToLong
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.example.project.models.CrearMovimientoInventarioRequest
import org.example.project.models.DevolucionParcialRequest
import org.example.project.models.EstadoVenta
import org.example.project.models.Producto
import org.example.project.models.ProductoVentaResponse
import org.example.project.models.TipoMovimientoInventario
import org.example.project.models.Usuario
import org.example.project.models.Venta
import org.example.project.models.VentaProducto
import org.example.project.models.VentaProductos
import org.example.project.models.VentaRequest
import org.example.project.models.VentaResponse
import org.example.project.models.Ventas
import org.example.project.models.VentasListResponse
import org.example.project.models.MetricasVentasResponse
import org.example.project.services.tieneSaldo
import org.example.project.services.totalNeto
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and

class VentasService {

    data class Filtros(
        val desde: LocalDateTime? = null,
        val hasta: LocalDateTime? = null,
        val vendedorId: Int? = null,
        val cliente: String? = null,
        val estado: EstadoVenta? = null
    )

    private data class ProductoVentaCalculado(
        val producto: Producto,
        val cantidad: Int,
        val precioUnitario: Long,
        val descuento: Long,
        val iva: Double,
        val subtotal: Long,
        val ivaMonto: Long
    )

    private val movimientosService = MovimientosInventarioService()
    private val diasLimiteDevolucion = 15

    fun crearVenta(request: VentaRequest): VentaResponse = transaction {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val descuentoGlobal = request.descuento?.coerceAtLeast(0) ?: 0L
        val clienteFormalReq = request.clienteFormal
        val clienteNombre = clienteFormalReq?.nombre?.ifBlank { request.cliente } ?: request.cliente

        if (clienteNombre.isBlank()) error("El cliente es requerido")
        if (request.productos.isEmpty()) error("Debe agregar al menos un producto")

        val productosCalculados = request.productos.map { item ->
            val producto = Producto.findById(item.id)
                ?: throw IllegalArgumentException("Producto con ID ${item.id} no encontrado")
            if (item.cantidad <= 0) error("Cantidad inválida para ${producto.nombre}")
            if (producto.cantidad < item.cantidad) {
                error("Stock insuficiente para ${producto.nombre}. Stock disponible: ${producto.cantidad}")
            }

            val precioUnitario = (item.precio ?: producto.precio).coerceAtLeast(0)
            val descuentoLinea = (item.descuento ?: 0L).coerceAtLeast(0)
            val ivaRate = (item.iva ?: 0.0).coerceAtLeast(0.0)
            val base = (precioUnitario * item.cantidad - descuentoLinea).coerceAtLeast(0)
            val ivaMonto = (base * ivaRate / 100.0).roundToLong()
            val subtotalLinea = base + ivaMonto

            ProductoVentaCalculado(
                producto = producto,
                cantidad = item.cantidad,
                precioUnitario = precioUnitario,
                descuento = descuentoLinea,
                iva = ivaRate,
                subtotal = subtotalLinea,
                ivaMonto = ivaMonto
            )
        }

        val subtotal = productosCalculados.sumOf { (it.precioUnitario * it.cantidad - it.descuento).coerceAtLeast(0) }
        val impuestos = productosCalculados.sumOf { it.ivaMonto }
        val totalSinDescuentoGlobal = subtotal + impuestos
        val total = (totalSinDescuentoGlobal - descuentoGlobal).coerceAtLeast(0)

        val venta = Venta.new {
            numero = generarNumeroCorrelativo()
            cliente = clienteNombre
            clienteRut = clienteFormalReq?.rut
            clienteDireccion = clienteFormalReq?.direccion
            clienteTelefono = clienteFormalReq?.telefono
            clienteEmail = clienteFormalReq?.email
            fecha = ahora
            this.subtotal = subtotal
            this.impuestos = impuestos
            this.total = total
            this.descuento = descuentoGlobal
            estado = EstadoVenta.PENDIENTE
            metodoPago = request.metodoPago
            observaciones = request.observaciones
            vendedor = request.vendedorId?.let { Usuario.findById(it) }
            fechaCreacion = ahora
            fechaActualizacion = ahora
        }

        val productosRespuesta = productosCalculados.map { linea ->
            VentaProducto.new {
                ventaId = venta.id
                productoId = linea.producto.id
                cantidad = linea.cantidad
                precio = linea.precioUnitario
                this.subtotal = linea.subtotal
                this.descuento = linea.descuento
                this.iva = linea.iva
                this.devuelto = 0
            }

            ProductoVentaResponse(
                id = linea.producto.id.value,
                nombre = linea.producto.nombre,
                cantidad = linea.cantidad,
                precio = linea.precioUnitario,
                subtotal = linea.subtotal,
                descuento = linea.descuento,
                iva = linea.iva,
                devuelto = 0
            )
        }

        venta.toResponse(productosRespuesta)
    }

    fun registrarDevolucionParcial(ventaId: String, request: DevolucionParcialRequest): VentaResponse = transaction {
        val venta = findVenta(ventaId) ?: throw IllegalArgumentException("Venta no encontrada")
        if (venta.estado != EstadoVenta.COMPLETADA) {
            error("Solo se pueden devolver ventas completadas")
        }

        val hoy = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val limite = venta.fecha.date.plus(DatePeriod(days = diasLimiteDevolucion))
        if (hoy.date > limite) {
            error("La venta superó el plazo de devolución (${diasLimiteDevolucion} días)")
        }

        if (request.items.isEmpty()) error("Debes indicar al menos un item a devolver")

        val productos = VentaProducto.find { VentaProductos.ventaId eq venta.id }
            .associateBy { it.productoId.value }

        request.items.forEach { item ->
            val vp = productos[item.productoId] ?: error("El producto ${item.productoId} no pertenece a la venta")
            if (item.cantidad <= 0) error("Cantidad inválida para devolución")

            val restante = vp.cantidad - vp.devuelto
            if (restante <= 0) error("El producto ${item.productoId} ya fue devuelto completamente")
            if (item.cantidad > restante) error("La devolución excede lo vendido para ${item.productoId}")

            vp.devuelto = vp.devuelto + item.cantidad

            movimientosService.crearMovimiento(
                CrearMovimientoInventarioRequest(
                    productoId = item.productoId,
                    tipo = TipoMovimientoInventario.ENTRADA,
                    cantidad = item.cantidad,
                    motivo = request.motivo ?: "Devolución parcial venta ${venta.numero}",
                    documento = venta.numero,
                    usuarioId = venta.vendedor?.id?.value,
                    observaciones = request.motivo,
                    fechaRegistro = hoy.toString()
                )
            )
        }

        venta.fechaActualizacion = hoy
        venta.toResponse(mapProductos(venta))
    }

    fun obtenerTodasLasVentas(): List<VentaResponse> = transaction {
        listarVentasInterno(Filtros())
    }

    fun obtenerVentasConMetricas(): VentasListResponse {
        val ventas = obtenerTodasLasVentas()
        val metricas = obtenerMetricas()
        return VentasListResponse(ventas = ventas, metricas = metricas)
    }

    fun listarVentas(
        filtros: Filtros = Filtros(),
        limit: Int = 200,
        offset: Int = 0
    ): List<VentaResponse> = transaction { listarVentasInterno(filtros, limit, offset) }

    fun obtenerVentaPorId(ventaId: String): VentaResponse? = transaction {
        val venta = findVenta(ventaId) ?: return@transaction null
        venta.toResponse(mapProductos(venta))
    }

    fun actualizarEstadoVenta(ventaId: String, nuevoEstado: EstadoVenta): VentaResponse = transaction {
        val venta = findVenta(ventaId) ?: throw IllegalArgumentException("Venta no encontrada")
        val estadoAnterior = venta.estado
        if (estadoAnterior == nuevoEstado) return@transaction venta.toResponse(mapProductos(venta))

        val productos = VentaProducto.find { VentaProductos.ventaId eq venta.id }.toList()
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        when (nuevoEstado) {
            EstadoVenta.CANCELADA -> {
                if (estadoAnterior == EstadoVenta.COMPLETADA) {
                    productos.forEach { vp ->
                        val pendiente = (vp.cantidad - vp.devuelto).coerceAtLeast(0)
                        if (pendiente > 0) {
                            movimientosService.crearMovimiento(
                                CrearMovimientoInventarioRequest(
                                    productoId = vp.productoId.value,
                                    tipo = TipoMovimientoInventario.ENTRADA,
                                    cantidad = pendiente,
                                    motivo = "Reverso venta ${venta.numero}",
                                    documento = venta.numero,
                                    usuarioId = venta.vendedor?.id?.value,
                                    fechaRegistro = ahora.toString()
                                )
                            )
                        }
                    }
                }
            }

            EstadoVenta.COMPLETADA -> {
                val faltantes = mutableListOf<String>()
                productos.forEach { vp ->
                    val producto = Producto.findById(vp.productoId) ?: return@forEach
                    val requerido = (vp.cantidad - vp.devuelto).coerceAtLeast(0)
                    if (requerido > 0 && producto.cantidad < requerido) {
                        faltantes += "${producto.nombre} (requiere $requerido, stock ${producto.cantidad})"
                    }
                }
                if (faltantes.isNotEmpty()) {
                    error("Stock insuficiente: ${faltantes.joinToString(", ")}")
                }

                if (estadoAnterior != EstadoVenta.COMPLETADA) {
                    productos.forEach { vp ->
                        val pendienteSalida = (vp.cantidad - vp.devuelto).coerceAtLeast(0)
                        if (pendienteSalida > 0) {
                            movimientosService.crearMovimiento(
                                CrearMovimientoInventarioRequest(
                                    productoId = vp.productoId.value,
                                    tipo = TipoMovimientoInventario.SALIDA,
                                    cantidad = pendienteSalida,
                                    motivo = "Venta ${venta.numero}",
                                    documento = venta.numero,
                                    usuarioId = venta.vendedor?.id?.value,
                                    fechaRegistro = ahora.toString()
                                )
                            )
                        }
                    }
                }
            }

            EstadoVenta.PENDIENTE -> {
                if (estadoAnterior == EstadoVenta.COMPLETADA) {
                    productos.forEach { vp ->
                        val pendiente = (vp.cantidad - vp.devuelto).coerceAtLeast(0)
                        if (pendiente > 0) {
                            movimientosService.crearMovimiento(
                                CrearMovimientoInventarioRequest(
                                    productoId = vp.productoId.value,
                                    tipo = TipoMovimientoInventario.ENTRADA,
                                    cantidad = pendiente,
                                    motivo = "Venta ${venta.numero} regresada a pendiente",
                                    documento = venta.numero,
                                    usuarioId = venta.vendedor?.id?.value,
                                    fechaRegistro = ahora.toString()
                                )
                            )
                        }
                    }
                }
            }
        }

        venta.estado = nuevoEstado
        venta.fechaActualizacion = ahora
        venta.toResponse(mapProductos(venta))
    }

    fun exportarVentasCsv(filtros: Filtros = Filtros()): String {
        val header = "Numero,Cliente,RUT,Fecha,Vendedor,Estado,Metodo,Subtotal,Impuestos,Descuento,Total,Devuelto,Saldo\n"
        val rows = listarVentas(filtros, limit = 5000).joinToString("\n") { venta ->
            listOf(
                venta.numero,
                venta.cliente,
                venta.clienteFormal?.rut ?: "",
                venta.fecha,
                venta.vendedorId ?: "",
                venta.estado,
                venta.metodoPago,
                venta.subtotal,
                venta.impuestos,
                venta.descuento,
                venta.total,
                venta.totalDevuelto,
                venta.saldo
            ).joinToString(",") { value ->
                val text = value.toString().replace("\"", "\"\"")
                "\"$text\""
            }
        }
        return header + rows
    }

    fun exportarVentasPdf(filtros: Filtros = Filtros()): ByteArray {
        val ventas = listarVentas(filtros, limit = 5000)
        val output = ByteArrayOutputStream()
        val document = Document(PageSize.A4.rotate())
        PdfWriter.getInstance(document, output)
        document.open()
        document.add(Paragraph("Reporte de ventas"))
        document.add(Paragraph("Generado: ${Clock.System.now().toLocalDateTime(TimeZone.UTC)}"))
        document.add(Paragraph("Total ventas: ${ventas.size}"))

        val table = PdfPTable(8)
        val headers = listOf("N°", "Cliente", "Fecha", "Estado", "Método", "Subtotal", "Impuestos", "Total")
        headers.forEach { table.addCell(it) }
        ventas.forEach { venta ->
            table.addCell(venta.numero)
            table.addCell(venta.cliente)
            table.addCell(venta.fecha)
            table.addCell(venta.estado.name)
            table.addCell(venta.metodoPago.name)
            table.addCell(venta.subtotal.toString())
            table.addCell(venta.impuestos.toString())
            table.addCell(venta.total.toString())
        }

        document.add(table)
        document.close()
        return output.toByteArray()
    }

    fun generarComprobantePdf(ventaId: String): ByteArray {
        val venta = obtenerVentaPorId(ventaId) ?: throw IllegalArgumentException("Venta no encontrada")
        val output = ByteArrayOutputStream()
        val document = Document(PageSize.A4)
        PdfWriter.getInstance(document, output)
        document.open()

        document.add(Paragraph("Comprobante de venta ${venta.numero}"))
        document.add(Paragraph("Cliente: ${venta.cliente}"))
        venta.clienteFormal?.rut?.let { document.add(Paragraph("RUT: $it")) }
        venta.clienteFormal?.direccion?.let { document.add(Paragraph("Dirección: $it")) }
        venta.clienteFormal?.telefono?.let { document.add(Paragraph("Teléfono: $it")) }
        venta.clienteFormal?.email?.let { document.add(Paragraph("Email: $it")) }
        document.add(Paragraph("Fecha: ${venta.fecha}"))
        document.add(Paragraph("Método de pago: ${venta.metodoPago.name}"))
        venta.observaciones?.let { document.add(Paragraph("Observaciones: $it")) }

        val table = PdfPTable(6)
        listOf("Producto", "Cant", "Precio", "Descuento", "IVA", "Total").forEach { table.addCell(it) }
        venta.productos.forEach { prod ->
            table.addCell(prod.nombre)
            table.addCell(prod.cantidad.toString())
            table.addCell(prod.precio.toString())
            table.addCell(prod.descuento.toString())
            table.addCell("${prod.iva}%")
            table.addCell(prod.subtotal.toString())
        }
        document.add(table)

        document.add(Paragraph("Subtotal: ${venta.subtotal}"))
        document.add(Paragraph("Impuestos: ${venta.impuestos}"))
        document.add(Paragraph("Descuento: ${venta.descuento}"))
        document.add(Paragraph("Total: ${venta.total}"))
        document.add(Paragraph("Devuelto: ${venta.totalDevuelto}"))
        document.add(Paragraph("Saldo: ${venta.saldo}"))

        document.close()
        return output.toByteArray()
    }

    fun generarComprobanteCsv(ventaId: String): String {
        val venta = obtenerVentaPorId(ventaId) ?: throw IllegalArgumentException("Venta no encontrada")
        val header = "Producto,Cantidad,Precio,Descuento,IVA,Total\n"
        val body = venta.productos.joinToString("\n") { prod ->
            listOf(
                prod.nombre,
                prod.cantidad,
                prod.precio,
                prod.descuento,
                prod.iva,
                prod.subtotal
            ).joinToString(",")
        }
        val resumen = listOf(
            "Subtotal,${venta.subtotal}",
            "Impuestos,${venta.impuestos}",
            "Descuento,${venta.descuento}",
            "Total,${venta.total}",
            "Devuelto,${venta.totalDevuelto}",
            "Saldo,${venta.saldo}"
        ).joinToString("\n")

        val clienteLinea = "Cliente,${venta.cliente},${venta.clienteFormal?.rut ?: ""},${venta.clienteFormal?.direccion ?: ""}"
        return "Venta ${venta.numero}\n$clienteLinea\n$header$body\n$resumen"
    }

    fun obtenerMetricas(): MetricasVentasResponse = transaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val startOfDay = LocalDateTime(now.year, now.monthNumber, now.dayOfMonth, 0, 0, 0)
        val endOfDay = startOfDay.plusDate(DatePeriod(days = 1))
        val startOfMonth = LocalDateTime(now.year, now.monthNumber, 1, 0, 0, 0)
        val startNextMonth = startOfMonth.plusDate(DatePeriod(months = 1))
        val startPrevDay = startOfDay.plusDate(DatePeriod(days = -1))
        val startPrevMonth = startOfMonth.plusDate(DatePeriod(months = -1))

        val hoyVentas = Venta.find {
            (Ventas.estado eq EstadoVenta.COMPLETADA) and
                (Ventas.fecha greaterEq startOfDay) and
                (Ventas.fecha less endOfDay)
        }.toList()
        val mesVentas = Venta.find {
            (Ventas.estado eq EstadoVenta.COMPLETADA) and
                (Ventas.fecha greaterEq startOfMonth) and
                (Ventas.fecha less startNextMonth)
        }.toList()
        val ayerVentas = Venta.find {
            (Ventas.estado eq EstadoVenta.COMPLETADA) and
                (Ventas.fecha greaterEq startPrevDay) and
                (Ventas.fecha less startOfDay)
        }.toList()
        val mesAnteriorVentas = Venta.find {
            (Ventas.estado eq EstadoVenta.COMPLETADA) and
                (Ventas.fecha greaterEq startPrevMonth) and
                (Ventas.fecha less startOfMonth)
        }.toList()

        val ventasHoy = hoyVentas.sumOf { it.totalNeto() }
        val ventasMes = mesVentas.sumOf { it.totalNeto() }
        val ordenesHoy = hoyVentas.count { it.tieneSaldo() }
        val ticketPromedio = if (ordenesHoy > 0) ventasHoy / ordenesHoy else ventasHoy
        val ventasAyer = ayerVentas.sumOf { it.totalNeto() }
        val ordenesAyer = ayerVentas.count { it.tieneSaldo() }
        val ticketAyer = if (ordenesAyer > 0) ventasAyer / ordenesAyer else ventasAyer
        val ventasMesAnterior = mesAnteriorVentas.sumOf { it.totalNeto() }

        MetricasVentasResponse(
            ventasHoy = ventasHoy,
            ordenesHoy = ordenesHoy,
            ticketPromedio = ticketPromedio,
            ventasMes = ventasMes,
            crecimientoVentasHoy = calcularCrecimientoPct(ventasHoy, ventasAyer),
            crecimientoOrdenes = calcularCrecimientoPct(ordenesHoy.toLong(), ordenesAyer.toLong()),
            crecimientoTicket = calcularCrecimientoPct(ticketPromedio, ticketAyer),
            crecimientoMes = calcularCrecimientoPct(ventasMes, ventasMesAnterior)
        )
    }

    private fun listarVentasInterno(
        filtros: Filtros,
        limit: Int = 200,
        offset: Int = 0
    ): List<VentaResponse> {
        var query = Ventas.selectAll()
        filtros.estado?.let { query = query.andWhere { Ventas.estado eq it } }
        filtros.desde?.let { query = query.andWhere { Ventas.fecha greaterEq it } }
        filtros.hasta?.let { query = query.andWhere { Ventas.fecha less it } }
        filtros.vendedorId?.let { query = query.andWhere { Ventas.vendedorId eq it } }
        filtros.cliente?.takeIf { it.isNotBlank() }?.let { cliente ->
            val pattern = "%${cliente.trim().lowercase()}%"
            query = query.andWhere { Ventas.cliente.lowerCase() like pattern }
        }

        return Venta.wrapRows(
            query.orderBy(Ventas.fecha to SortOrder.DESC).limit(limit, offset.toLong())
        ).map { venta -> venta.toResponse(mapProductos(venta)) }
    }

    private fun calcularCrecimientoPct(actual: Long, anterior: Long): Int {
        if (anterior <= 0L) return if (actual > 0) 100 else 0
        val delta = actual - anterior
        return ((delta.toDouble() / anterior.toDouble()) * 100).toInt()
    }

    private fun findVenta(ventaId: String): Venta? {
        val numeroId = ventaId.removePrefix("V").toIntOrNull()
            ?: throw IllegalArgumentException("Formato de ID inválido")
        return Venta.findById(numeroId)
    }

    private fun mapProductos(venta: Venta): List<ProductoVentaResponse> =
        VentaProducto.find { VentaProductos.ventaId eq venta.id }.map { vp ->
            val producto = Producto.findById(vp.productoId)!!
            ProductoVentaResponse(
                id = producto.id.value,
                nombre = producto.nombre,
                cantidad = vp.cantidad,
                precio = vp.precio,
                subtotal = vp.subtotal,
                descuento = vp.descuento,
                iva = vp.iva,
                devuelto = vp.devuelto
            )
        }
}

private fun LocalDateTime.plusDate(period: DatePeriod): LocalDateTime =
    LocalDateTime(this.date.plus(period), this.time)

private fun generarNumeroCorrelativo(): String {
    val ultimo = Venta.all().orderBy(Ventas.id to SortOrder.DESC).limit(1).firstOrNull()
    val ultimoCorrelativo = ultimo?.numero?.removePrefix("V")?.toIntOrNull()
        ?: ultimo?.id?.value
        ?: 0
    val siguiente = ultimoCorrelativo + 1
    return "V${siguiente.toString().padStart(5, '0')}"
}

private fun Venta.toResponse(productos: List<ProductoVentaResponse>): VentaResponse {
    val subtotalCalculado = if (subtotal == 0L && productos.isNotEmpty()) productos.sumOf { it.subtotal } else subtotal
    val totalDevuelto = calcularTotalDevuelto(productos)
    val clienteFormal = clienteFormal()

    return VentaResponse(
        id = numero,
        numero = numero,
        cliente = cliente,
        clienteFormal = clienteFormal,
        fecha = fecha.toString() + "Z",
        subtotal = subtotalCalculado,
        impuestos = impuestos,
        total = total,
        descuento = descuento,
        estado = estado,
        metodoPago = metodoPago,
        vendedorId = vendedor?.id?.value,
        observaciones = observaciones,
        totalDevuelto = totalDevuelto,
        productos = productos
    )
}

private fun Venta.clienteFormal(): org.example.project.models.ClienteFormal? {
    val hayDatosExtra = listOf(clienteRut, clienteDireccion, clienteTelefono, clienteEmail).any { !it.isNullOrBlank() }
    return if (!hayDatosExtra && cliente.isBlank()) null else org.example.project.models.ClienteFormal(
        nombre = cliente,
        rut = clienteRut,
        direccion = clienteDireccion,
        telefono = clienteTelefono,
        email = clienteEmail
    )
}

private fun calcularTotalDevuelto(productos: List<ProductoVentaResponse>): Long =
    productos.sumOf { prod ->
        if (prod.devuelto <= 0 || prod.cantidad == 0) 0L
        else ((prod.subtotal.toDouble() / prod.cantidad.toDouble()) * prod.devuelto).roundToLong()
    }
