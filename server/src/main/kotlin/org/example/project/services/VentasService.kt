package org.example.project.services

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.example.project.models.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

class VentasService {
    private val movimientosService = MovimientosInventarioService()

    fun crearVenta(request: VentaRequest): VentaResponse = transaction {
        val ahora = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val descuento = request.descuento?.coerceAtLeast(0) ?: 0L

        // Validar que todos los productos existen y tienen stock suficiente
        request.productos.forEach { productoReq ->
            val producto = Producto.findById(productoReq.id)
                ?: throw IllegalArgumentException("Producto con ID ${productoReq.id} no encontrado")

            if (producto.cantidad < productoReq.cantidad) {
                throw IllegalArgumentException("Stock insuficiente para ${producto.nombre}. Stock disponible: ${producto.cantidad}")
            }
        }

        // Calcular total
        val total = request.productos.sumOf {
            val producto = Producto.findById(it.id)
                ?: throw IllegalArgumentException("Producto con ID ${it.id} no encontrado")
            producto.precio * it.cantidad
        }
        val totalConDescuento = (total - descuento).coerceAtLeast(0)

        // Crear la venta
        val venta = Venta.new {
            numero = generarNumeroCorrelativo()
            cliente = request.cliente
            fecha = ahora
            this.total = totalConDescuento
            this.descuento = descuento
            estado = EstadoVenta.PENDIENTE
            metodoPago = request.metodoPago
            observaciones = request.observaciones
            vendedor = request.vendedorId?.let { Usuario.findById(it) }
            fechaCreacion = ahora
            fechaActualizacion = ahora
        }

        // Crear los productos de la venta y actualizar stock
        val productosVenta = request.productos.map { productoReq ->
            val producto = Producto.findById(productoReq.id)!!
            val subtotal = producto.precio * productoReq.cantidad

            // Crear registro en venta_productos
            VentaProducto.new {
                ventaId = venta.id
                productoId = producto.id
                cantidad = productoReq.cantidad
                precio = producto.precio
                this.subtotal = subtotal
            }

            // Registrar movimiento de salida
            movimientosService.crearMovimiento(
                CrearMovimientoInventarioRequest(
                    productoId = producto.id.value,
                    tipo = TipoMovimientoInventario.SALIDA,
                    cantidad = productoReq.cantidad,
                    motivo = "Venta ${venta.numero}",
                    documento = venta.numero,
                    usuarioId = request.vendedorId,
                    observaciones = request.observaciones,
                    fechaRegistro = ahora.toString()
                )
            )

            ProductoVentaResponse(
                id = producto.id.value,
                nombre = producto.nombre,
                cantidad = productoReq.cantidad,
                precio = producto.precio,
                subtotal = subtotal
            )
        }

        venta.toResponse(productosVenta)
    }

    fun obtenerTodasLasVentas(): List<VentaResponse> = transaction {
        Venta.all().orderBy(Ventas.fechaCreacion to SortOrder.DESC).map { venta ->
            val productos = VentaProducto.find { VentaProductos.ventaId eq venta.id }.map { vp ->
                val producto = Producto.findById(vp.productoId)!!
                ProductoVentaResponse(
                    id = producto.id.value,
                    nombre = producto.nombre,
                    cantidad = vp.cantidad,
                    precio = vp.precio,
                    subtotal = vp.subtotal
                )
            }

            venta.toResponse(productos)
        }
    }

    fun obtenerVentasConMetricas(): VentasListResponse {
        val ventas = obtenerTodasLasVentas()
        val metricas = obtenerMetricas()
        return VentasListResponse(ventas = ventas, metricas = metricas)
    }

    fun obtenerVentaPorId(ventaId: String): VentaResponse? = transaction {
        // Extraer el número del ID (V001 -> 1)
        val numeroId = ventaId.removePrefix("V").toIntOrNull()
            ?: throw IllegalArgumentException("Formato de ID inválido")

        val venta = Venta.findById(numeroId) ?: return@transaction null

        val productos = VentaProducto.find { VentaProductos.ventaId eq venta.id }.map { vp ->
            val producto = Producto.findById(vp.productoId)!!
            ProductoVentaResponse(
                id = producto.id.value,
                nombre = producto.nombre,
                cantidad = vp.cantidad,
                precio = vp.precio,
                subtotal = vp.subtotal
            )
        }

        venta.toResponse(productos)
    }

    fun actualizarEstadoVenta(ventaId: String, nuevoEstado: EstadoVenta): VentaResponse = transaction {
        val numeroId = ventaId.removePrefix("V").toIntOrNull()
            ?: throw IllegalArgumentException("Formato de ID inválido")

        val venta = Venta.findById(numeroId)
            ?: throw IllegalArgumentException("Venta no encontrada")

        val estadoAnterior = venta.estado
        venta.estado = nuevoEstado
        venta.fechaActualizacion = Clock.System.now().toLocalDateTime(TimeZone.UTC)

        // Si se cancela la venta, restaurar el stock
        if (nuevoEstado == EstadoVenta.CANCELADA && estadoAnterior != EstadoVenta.CANCELADA) {
            VentaProducto.find { VentaProductos.ventaId eq venta.id }.forEach { vp ->
                val producto = Producto.findById(vp.productoId)!!
                movimientosService.crearMovimiento(
                    CrearMovimientoInventarioRequest(
                        productoId = producto.id.value,
                        tipo = TipoMovimientoInventario.ENTRADA,
                        cantidad = vp.cantidad,
                        motivo = "Reverso venta ${venta.numero}",
                        documento = venta.numero,
                        usuarioId = venta.vendedor?.id?.value,
                        fechaRegistro = Clock.System.now().toLocalDateTime(TimeZone.UTC).toString()
                    )
                )
            }
        }

        obtenerVentaPorId(ventaId)!!
    }

    fun obtenerMetricas(): MetricasVentasResponse = transaction {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val startOfDay = LocalDateTime(now.year, now.monthNumber, now.dayOfMonth, 0, 0, 0)
        val endOfDay = startOfDay.plusDate(DatePeriod(days = 1))
        val startOfMonth = LocalDateTime(now.year, now.monthNumber, 1, 0, 0, 0)
        val startNextMonth = startOfMonth.plusDate(DatePeriod(months = 1))

        val hoyQuery = Venta.find {
            (Ventas.estado neq EstadoVenta.CANCELADA) and
                (Ventas.fecha greaterEq startOfDay) and
                (Ventas.fecha less endOfDay)
        }
        val mesQuery = Venta.find {
            (Ventas.estado neq EstadoVenta.CANCELADA) and
                (Ventas.fecha greaterEq startOfMonth) and
                (Ventas.fecha less startNextMonth)
        }

        val ventasHoy = hoyQuery.sumOf { it.total }
        val ventasMes = mesQuery.sumOf { it.total }
        val ordenesHoy = hoyQuery.count().toInt()
        val ticketPromedio = if (ordenesHoy > 0) ventasHoy / ordenesHoy else ventasHoy

        MetricasVentasResponse(
            ventasHoy = ventasHoy,
            ordenesHoy = ordenesHoy,
            ticketPromedio = ticketPromedio,
            ventasMes = ventasMes,
            crecimientoVentasHoy = 0,
            crecimientoOrdenes = 0,
            crecimientoTicket = 0,
            crecimientoMes = 0
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

private fun Venta.toResponse(productos: List<ProductoVentaResponse>): VentaResponse =
    VentaResponse(
        id = numero,
        numero = numero,
        cliente = cliente,
        fecha = fecha.toString() + "Z",
        total = total,
        descuento = descuento,
        estado = estado,
        metodoPago = metodoPago,
        vendedorId = vendedor?.id?.value,
        observaciones = observaciones,
        productos = productos
    )
