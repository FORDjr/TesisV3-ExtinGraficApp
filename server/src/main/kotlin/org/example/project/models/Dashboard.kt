package org.example.project.models

import kotlinx.serialization.Serializable

@Serializable
data class DashboardSummaryResponse(
    val generatedAt: Long,
    val scope: DashboardScope,
    val ventas: DashboardVentasBlock,
    val inventario: DashboardInventarioBlock,
    val alertas: DashboardAlertasBlock
)

@Serializable
data class DashboardScope(
    val clienteId: Int? = null,
    val sedeId: Int? = null
)

@Serializable
data class DashboardVentasBlock(
    val hoy: Long,
    val mes: Long,
    val ordenesHoy: Int,
    val ticketPromedio: Long,
    val crecimiento: DashboardCrecimientoBlock
)

@Serializable
data class DashboardCrecimientoBlock(
    val ventasHoyPct: Int,
    val ordenesPct: Int,
    val ticketPct: Int,
    val mesPct: Int
)

@Serializable
data class DashboardInventarioBlock(
    val totalProductos: Int,
    val stockCritico: Int,
    val extintores: DashboardExtintoresBlock
)

@Serializable
data class DashboardExtintoresBlock(
    val total: Int,
    val rojo: Int,
    val amarillo: Int,
    val verde: Int,
    val vencen30: Int
)

@Serializable
data class DashboardAlertasBlock(
    val pendientes: Int,
    val porTipo: List<DashboardAlertasPorTipo>
)

@Serializable
data class DashboardAlertasPorTipo(
    val tipo: String,
    val cantidad: Int
)
