package org.example.project.routes

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import org.example.project.models.*
import org.example.project.services.AlertService
import org.example.project.services.ExtintoresService
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction

private object DashboardCache {
    data class Key(val clienteId: Int?, val sedeId: Int?)
    data class Entry(val key: Key, val timestampMs: Long, val payload: DashboardSummaryResponse)
    var last: Entry? = null
    const val TTL_MS = 60_000L
}

fun Route.dashboardRoutes() {
    val extService = ExtintoresService()
    val alertService = AlertService(extService)

    route("/api/dashboard") {
        get {
            val clienteId = call.request.queryParameters["clienteId"]?.toIntOrNull()
            val sedeId = call.request.queryParameters["sedeId"]?.toIntOrNull()
            val key = DashboardCache.Key(clienteId, sedeId)
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
                alertService = alertService
            )

            DashboardCache.last = DashboardCache.Entry(key, nowMs, payload)
            call.respond(payload)
        }

        // Alias opcional para claridad
        get("/resumen") {
            val clienteId = call.request.queryParameters["clienteId"]?.toIntOrNull()
            val sedeId = call.request.queryParameters["sedeId"]?.toIntOrNull()
            val key = DashboardCache.Key(clienteId, sedeId)
            val nowMs = System.currentTimeMillis()
            val payload = buildDashboardResponse(
                key = key,
                nowMs = nowMs,
                extService = extService,
                alertService = alertService
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
    alertService: AlertService
): DashboardSummaryResponse {
    val scope = DashboardScope(clienteId = key.clienteId, sedeId = key.sedeId)

    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val startOfDay = LocalDateTime(now.year, now.monthNumber, now.dayOfMonth, 0, 0, 0)
    val endOfDay = startOfDay.plusDate(DatePeriod(days = 1))
    val startOfMonth = LocalDateTime(now.year, now.monthNumber, 1, 0, 0, 0)
    val startNextMonth = startOfMonth.plusDate(DatePeriod(months = 1))

    val ventasStats = transaction {
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
        val ordenesHoy = hoyQuery.count().toInt()
        val ventasMes = mesQuery.sumOf { it.total }
        val ticketPromedio = if (ordenesHoy > 0) ventasHoy / ordenesHoy else ventasHoy

        DashboardVentasBlock(
            hoy = ventasHoy,
            mes = ventasMes,
            ordenesHoy = ordenesHoy,
            ticketPromedio = ticketPromedio,
            crecimiento = DashboardCrecimientoBlock(
                ventasHoyPct = 0,
                ordenesPct = 0,
                ticketPct = 0,
                mesPct = 0
            )
        )
    }

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

    val alertasBlock = DashboardAlertasBlock(
        pendientes = pendientes.size,
        porTipo = porTipo
    )

    return DashboardSummaryResponse(
        generatedAt = nowMs,
        scope = scope,
        ventas = ventasStats,
        inventario = inventarioBlock,
        alertas = alertasBlock
    )
}

private fun LocalDateTime.plusDate(period: DatePeriod): LocalDateTime =
    LocalDateTime(this.date.plus(period), this.time)
