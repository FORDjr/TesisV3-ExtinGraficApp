package org.example.project.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import io.ktor.http.HttpStatusCode
import org.example.project.models.*
import org.example.project.services.AlertService
import org.example.project.services.ExtintoresService
import org.example.project.services.tieneSaldo
import org.example.project.services.totalNeto
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

private object DashboardCache {
    data class Key(val clienteId: Int?, val sedeId: Int?, val desde: String?, val hasta: String?)
    data class Entry(val key: Key, val timestampMs: Long, val payload: DashboardSummaryResponse)
    var last: Entry? = null
    const val TTL_MS = 60_000L
}

private data class DashboardFilters(val desde: LocalDate?, val hasta: LocalDate?)

fun Route.dashboardRoutes() {
    val extService = ExtintoresService()
    val alertService = AlertService(extService)

    route("/api/dashboard") {
        get {
            val clienteId = call.request.queryParameters["clienteId"]?.toIntOrNull()
            val sedeId = call.request.queryParameters["sedeId"]?.toIntOrNull()
            val desdeParam = call.request.queryParameters["desde"]
            val hastaParam = call.request.queryParameters["hasta"]
            val desdeDate = parseDateOrNull(desdeParam)
            val hastaDate = parseDateOrNull(hastaParam)
            if (desdeParam != null && desdeDate == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Formato inválido para 'desde' (use yyyy-MM-dd)"))
                return@get
            }
            if (hastaParam != null && hastaDate == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Formato inválido para 'hasta' (use yyyy-MM-dd)"))
                return@get
            }
            val filtros = DashboardFilters(desdeDate, hastaDate)
            val key = DashboardCache.Key(clienteId, sedeId, desdeParam, hastaParam)
            val nowMs = System.currentTimeMillis()
            DashboardCache.last?.let { cached ->
                if (cached.key == key && (nowMs - cached.timestampMs) < DashboardCache.TTL_MS) {
                    call.respond(cached.payload)
                    return@get
                }
            }

            val payload = buildDashboardResponse(
                key = key,
                nowMs = nowMs,
                extService = extService,
                alertService = alertService,
                filtros = filtros
            )

            DashboardCache.last = DashboardCache.Entry(key, nowMs, payload)
            call.respond(payload)
        }

        // Alias opcional para claridad
        get("/resumen") {
            val clienteId = call.request.queryParameters["clienteId"]?.toIntOrNull()
            val sedeId = call.request.queryParameters["sedeId"]?.toIntOrNull()
            val desdeParam = call.request.queryParameters["desde"]
            val hastaParam = call.request.queryParameters["hasta"]
            val desdeDate = parseDateOrNull(desdeParam)
            val hastaDate = parseDateOrNull(hastaParam)
            if (desdeParam != null && desdeDate == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Formato inválido para 'desde' (use yyyy-MM-dd)"))
                return@get
            }
            if (hastaParam != null && hastaDate == null) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Formato inválido para 'hasta' (use yyyy-MM-dd)"))
                return@get
            }
            val filtros = DashboardFilters(desdeDate, hastaDate)
            val key = DashboardCache.Key(clienteId, sedeId, desdeParam, hastaParam)
            val nowMs = System.currentTimeMillis()
            val payload = buildDashboardResponse(
                key = key,
                nowMs = nowMs,
                extService = extService,
                alertService = alertService,
                filtros = filtros
            )
            DashboardCache.last = DashboardCache.Entry(key, nowMs, payload)
            call.respond(payload)
        }
    }
}

private fun buildDashboardResponse(
    key: DashboardCache.Key,
    nowMs: Long,
    extService: ExtintoresService,
    alertService: AlertService,
    filtros: DashboardFilters
): DashboardSummaryResponse {
    val scope = DashboardScope(
        clienteId = key.clienteId,
        sedeId = key.sedeId,
        desde = key.desde,
        hasta = key.hasta
    )

    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val startOfDay = LocalDateTime(now.year, now.monthNumber, now.dayOfMonth, 0, 0, 0)
    val endOfDay = startOfDay.plusDate(DatePeriod(days = 1))
    val startPrevDay = startOfDay.plusDate(DatePeriod(days = -1))
    val startOfMonth = LocalDateTime(now.year, now.monthNumber, 1, 0, 0, 0)
    val startNextMonth = startOfMonth.plusDate(DatePeriod(months = 1))
    val startPrevMonth = startOfMonth.plusDate(DatePeriod(months = -1))
    val rangeStart = filtros.desde?.let { LocalDateTime(it, LocalTime(0, 0, 0)) } ?: startOfMonth
    val rangeEnd = filtros.hasta?.plus(DatePeriod(days = 1))
        ?.let { LocalDateTime(it, LocalTime(0, 0, 0)) } ?: startNextMonth

    val ventasStats = transaction {
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
        val rangoVentas = Venta.find {
            (Ventas.estado eq EstadoVenta.COMPLETADA) and
                (Ventas.fecha greaterEq rangeStart) and
                (Ventas.fecha less rangeEnd)
        }.toList()

        val ventasHoy = hoyVentas.sumOf { it.totalNeto() }
        val ordenesHoy = hoyVentas.count { it.tieneSaldo() }
        val ventasMes = mesVentas.sumOf { it.totalNeto() }
        val ventasRango = rangoVentas.sumOf { it.totalNeto() }
        val ordenesRango = rangoVentas.count { it.tieneSaldo() }
        val ticketRango = if (ordenesRango > 0) ventasRango / ordenesRango else ventasRango

        val ventasAyer = ayerVentas.sumOf { it.totalNeto() }
        val ordenesAyer = ayerVentas.count { it.tieneSaldo() }
        val ticketAyer = if (ordenesAyer > 0) ventasAyer / ordenesAyer else ventasAyer
        val ventasMesAnterior = mesAnteriorVentas.sumOf { it.totalNeto() }

        val topProductos = calcularTopProductos(rangoVentas)
        val serie = calcularSerieVentas(rangoVentas)

        DashboardVentasBlock(
            hoy = ventasHoy,
            mes = ventasMes,
            ordenesHoy = ordenesHoy,
            ticketPromedio = ticketRango,
            crecimiento = DashboardCrecimientoBlock(
                ventasHoyPct = calcularCrecimientoPct(ventasHoy, ventasAyer),
                ordenesPct = calcularCrecimientoPct(ordenesHoy.toLong(), ordenesAyer.toLong()),
                ticketPct = calcularCrecimientoPct(ticketRango, ticketAyer),
                mesPct = calcularCrecimientoPct(ventasMes, ventasMesAnterior)
            ),
            rango = DashboardVentasRango(
                total = ventasRango,
                ordenes = ordenesRango,
                ticketPromedio = ticketRango
            ),
            topProductos = topProductos,
            serie = serie
        )
    }

    extService.recalcularEstados()
    val extintores = extService.listarExtintores(key.clienteId, key.sedeId, null)
    val inventarioBlock = transaction {
        val totalProductos = Producto.all().count().toInt()
        val stockCritico = Producto.all().count { it.cantidad <= it.stockMinimo }
        DashboardInventarioBlock(
            totalProductos = totalProductos,
            stockCritico = stockCritico,
            extintores = DashboardExtintoresBlock(
                total = extintores.size,
                rojo = extintores.count { it.color.equals("rojo", ignoreCase = true) },
                amarillo = extintores.count { it.color.equals("amarillo", ignoreCase = true) },
                verde = extintores.count { it.color.equals("verde", ignoreCase = true) },
                vencen30 = extintores.count { it.diasParaVencer != null && it.diasParaVencer <= 30 }
            )
        )
    }

    val pendientes = alertService.listar(true)
    val porTipo = pendientes.groupBy { it.tipo.uppercase() }
        .map { DashboardAlertasPorTipo(tipo = it.key, cantidad = it.value.size) }
    val movimientosPendientes = transaction {
        MovimientoInventario.find {
            MovimientosInventario.estadoAprobacion eq EstadoAprobacionMovimiento.PENDIENTE
        }.count().toInt()
    }

    val alertasBlock = DashboardAlertasBlock(
        pendientes = pendientes.size,
        porTipo = porTipo,
        stockCritico = inventarioBlock.stockCritico,
        vencimientosProximos = inventarioBlock.extintores.vencen30,
        movimientosPendientes = movimientosPendientes
    )

    return DashboardSummaryResponse(
        generatedAt = nowMs,
        scope = scope,
        ventas = ventasStats,
        inventario = inventarioBlock,
        alertas = alertasBlock
    )
}

private fun calcularCrecimientoPct(actual: Long, anterior: Long): Int {
    if (anterior <= 0L) return if (actual > 0) 100 else 0
    val delta = actual - anterior
    return ((delta.toDouble() / anterior.toDouble()) * 100).toInt()
}

private fun LocalDateTime.plusDate(period: DatePeriod): LocalDateTime =
    LocalDateTime(this.date.plus(period), this.time)

private fun parseDateOrNull(raw: String?): LocalDate? =
    raw?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

private fun calcularTopProductos(ventas: Iterable<Venta>): List<DashboardTopProducto> {
    data class Acc(val nombre: String, var cantidad: Int, var monto: Long)
    val acumulado = mutableMapOf<Int, Acc>()
    ventas.forEach { venta ->
        VentaProducto.find { VentaProductos.ventaId eq venta.id }.forEach { vp ->
            val prod = Producto.findById(vp.productoId.value) ?: return@forEach
            val cantidadVendida = vp.cantidad - vp.devuelto
            if (cantidadVendida <= 0) return@forEach
            val montoLinea = (vp.precio * cantidadVendida) - vp.descuento
            val acc = acumulado.getOrPut(prod.id.value) { Acc(prod.nombre, 0, 0) }
            acc.cantidad += cantidadVendida
            acc.monto += montoLinea
        }
    }
    return acumulado
        .map { (id, acc) -> DashboardTopProducto(id, acc.nombre, acc.cantidad, acc.monto) }
        .sortedWith(compareByDescending<DashboardTopProducto> { it.cantidad }.thenByDescending { it.monto })
        .take(5)
}

private fun calcularSerieVentas(ventas: Iterable<Venta>): List<DashboardSerieValor> {
    val porDia = mutableMapOf<LocalDate, Long>()
    ventas.forEach { venta ->
        val neto = venta.totalNeto()
        if (neto <= 0) return@forEach
        val dia = venta.fecha.date
        porDia[dia] = (porDia[dia] ?: 0L) + neto
    }
    return porDia
        .toSortedMap()
        .entries
        .toList()
        .takeLast(7)
        .map { (dia, total) ->
            DashboardSerieValor(
                label = dia.toString(),
                valor = total
            )
        }
}
