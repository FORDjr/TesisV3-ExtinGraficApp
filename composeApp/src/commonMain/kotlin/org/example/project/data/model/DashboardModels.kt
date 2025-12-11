package org.example.project.data.model

import kotlinx.serialization.Serializable

@Serializable
data class DashboardResponse(
    val generatedAt: Long,
    val scope: DashboardScope,
    val ventas: DashboardVentasBlock,
    val inventario: DashboardInventarioBlock,
    val alertas: DashboardAlertasBlock
)

@Serializable
data class DashboardScope(
    val clienteId: Int? = null,
    val sedeId: Int? = null,
    val desde: String? = null,
    val hasta: String? = null
)

@Serializable
data class DashboardVentasBlock(
    val hoy: Long,
    val mes: Long,
    val ordenesHoy: Int,
    val ticketPromedio: Long,
    val crecimiento: DashboardCrecimientoBlock,
    val rango: DashboardVentasRango = DashboardVentasRango(),
    val topProductos: List<DashboardTopProducto> = emptyList(),
    val serie: List<DashboardSerieValor> = emptyList()
)

@Serializable
data class DashboardCrecimientoBlock(
    val ventasHoyPct: Int,
    val ordenesPct: Int,
    val ticketPct: Int,
    val mesPct: Int
)

@Serializable
data class DashboardVentasRango(
    val total: Long = 0,
    val ordenes: Int = 0,
    val ticketPromedio: Long = 0
)

@Serializable
data class DashboardTopProducto(
    val productoId: Int,
    val nombre: String,
    val cantidad: Int,
    val monto: Long
)

@Serializable
data class DashboardSerieValor(
    val label: String,
    val valor: Long
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
    val porTipo: List<DashboardAlertasPorTipo>,
    val stockCritico: Int = 0,
    val vencimientosProximos: Int = 0,
    val movimientosPendientes: Int = 0
)

@Serializable
data class DashboardAlertasPorTipo(
    val tipo: String,
    val cantidad: Int
)

@Serializable
data class AlertaDto(
    val id: Int,
    val extintorId: Int? = null,
    val productoId: Int? = null,
    val tipo: String,
    val fechaGenerada: String,
    val enviada: Boolean,
    val reintentos: Int
)

@Serializable
data class ExtintorVencimiento(
    val id: Int,
    val codigo: String,
    val cliente: String = "",
    val sede: String = "",
    val dias: Int,
    val color: String
)

data class DashboardFilters(
    val clienteId: String = "",
    val sedeId: String = "",
    val desde: String? = null,
    val hasta: String? = null
)

enum class DashboardPreset { HOY, ULTIMOS_7, MES_ACTUAL, PERSONALIZADO }

data class DashboardUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val data: DashboardResponse? = null,
    val alertasPendientes: List<AlertaDto> = emptyList(),
    val alertasLoading: Boolean = false,
    val alertasError: String? = null,
    val serviciosHoyAgendados: Int = 0,
    val serviciosHoyPendientes: Int = 0,
    val extintoresVencimientoLista: List<ExtintorVencimiento> = emptyList(),
    val filtros: DashboardFilters = DashboardFilters(),
    val preset: DashboardPreset = DashboardPreset.MES_ACTUAL
)
