package org.example.project.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.example.project.data.api.DashboardApiService
import org.example.project.data.model.DashboardFilters
import org.example.project.data.model.DashboardPreset
import org.example.project.data.model.DashboardUiState
import org.example.project.data.model.ExtintorVencimiento
import org.example.project.data.api.CalendarApiService
import org.example.project.data.model.CalendarEvent
import org.example.project.utils.NotificationPreferences
import org.example.project.utils.Notifier

class DashboardViewModel(
    private val api: DashboardApiService = DashboardApiService()
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState(loading = true))
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()
    private val calendarApi = CalendarApiService()
    private var lastVentasHoy: Long = 0
    private var lastStockCritico: Int = 0

    init {
        val baseFilters = defaultFilters()
        _state.value = _state.value.copy(filtros = baseFilters, preset = DashboardPreset.MES_ACTUAL)
        refreshAll(desde = baseFilters.desde, hasta = baseFilters.hasta)
    }

    fun refreshAll(
        clienteId: Int? = _state.value.filtros.clienteId.toIntOrNull(),
        sedeId: Int? = _state.value.filtros.sedeId.toIntOrNull(),
        desde: String? = _state.value.filtros.desde,
        hasta: String? = _state.value.filtros.hasta
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                loading = true,
                error = null,
                filtros = _state.value.filtros.copy(
                    clienteId = clienteId?.toString() ?: _state.value.filtros.clienteId,
                    sedeId = sedeId?.toString() ?: _state.value.filtros.sedeId,
                    desde = desde,
                    hasta = hasta
                )
            )
            try {
                val dash = api.fetchDashboard(clienteId, sedeId, desde, hasta)
                val alertas = api.listarAlertasPendientes()
                val eventos = runCatching { calendarApi.obtenerEventosCalendario() }.getOrElse { emptyList() }
                val extList = eventosPorVencer(eventos, dash.inventario.extintores.vencen30)
                val serviciosHoy = eventos.count { it.type.equals("ORDEN", ignoreCase = true) && (it.daysToExpire ?: Long.MAX_VALUE) == 0L }
                val pendientesHoy = eventos.count { it.type.equals("EXTINTOR", ignoreCase = true) && (it.daysToExpire ?: Long.MAX_VALUE) == 0L }
                triggerDashboardNotifications(dash)
                _state.value = _state.value.copy(
                    loading = false,
                    data = dash,
                    alertasPendientes = alertas,
                    alertasLoading = false,
                    error = null,
                    serviciosHoyAgendados = serviciosHoy,
                    serviciosHoyPendientes = pendientesHoy,
                    extintoresVencimientoLista = extList
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
    }

    private fun defaultFilters(): DashboardFilters {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        val startMonth = LocalDate(now.year, now.monthNumber, 1)
        return DashboardFilters(
            desde = startMonth.toString(),
            hasta = now.toString()
        )
    }

    private fun rangeForPreset(preset: DashboardPreset): Pair<String, String> {
        val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
        return when (preset) {
            DashboardPreset.HOY -> today.toString() to today.toString()
            DashboardPreset.ULTIMOS_7 -> today.minus(DatePeriod(days = 6)).toString() to today.toString()
            DashboardPreset.MES_ACTUAL -> LocalDate(today.year, today.monthNumber, 1).toString() to today.toString()
            DashboardPreset.PERSONALIZADO -> (_state.value.filtros.desde ?: today.toString()) to (_state.value.filtros.hasta ?: today.toString())
        }
    }

    fun applyPreset(preset: DashboardPreset) {
        val (desde, hasta) = rangeForPreset(preset)
        val filtros = _state.value.filtros.copy(desde = desde, hasta = hasta)
        _state.value = _state.value.copy(filtros = filtros, preset = preset)
        refreshAll(desde = desde, hasta = hasta)
    }

    fun updateClienteFiltro(value: String) {
        _state.value = _state.value.copy(
            filtros = _state.value.filtros.copy(clienteId = value),
            preset = DashboardPreset.PERSONALIZADO
        )
    }

    fun updateSedeFiltro(value: String) {
        _state.value = _state.value.copy(
            filtros = _state.value.filtros.copy(sedeId = value),
            preset = DashboardPreset.PERSONALIZADO
        )
    }

    fun updateDesdeFiltro(value: String) {
        _state.value = _state.value.copy(
            filtros = _state.value.filtros.copy(desde = value),
            preset = DashboardPreset.PERSONALIZADO
        )
    }

    fun updateHastaFiltro(value: String) {
        _state.value = _state.value.copy(
            filtros = _state.value.filtros.copy(hasta = value),
            preset = DashboardPreset.PERSONALIZADO
        )
    }

    fun aplicarFiltrosManuales() {
        val filtros = _state.value.filtros
        _state.value = _state.value.copy(preset = DashboardPreset.PERSONALIZADO)
        refreshAll(
            clienteId = filtros.clienteId.toIntOrNull(),
            sedeId = filtros.sedeId.toIntOrNull(),
            desde = filtros.desde,
            hasta = filtros.hasta
        )
    }

    private fun eventosPorVencer(eventos: List<CalendarEvent>, fallbackCount: Int): List<ExtintorVencimiento> {
        val list = eventos.filter { it.type.equals("EXTINTOR", ignoreCase = true) }
            .filter { (it.daysToExpire ?: Long.MAX_VALUE) <= 30 }
            .map { ev ->
                ExtintorVencimiento(
                    id = ev.referenceId ?: ev.id,
                    codigo = ev.codigo ?: ev.title,
                    cliente = ev.cliente ?: "",
                    sede = ev.sede ?: "",
                    dias = (ev.daysToExpire ?: 0L).toInt(),
                    color = ev.color
                )
            }
        if (list.isNotEmpty()) return list
        return mockExtintoresVencimiento(fallbackCount)
    }

    private fun triggerDashboardNotifications(dash: org.example.project.data.model.DashboardResponse) {
        val prefs = NotificationPreferences.settings.value
        if (prefs.notifySales && dash.ventas.hoy > lastVentasHoy) {
            Notifier.notify("Nueva venta registrada", "Ventas hoy: ${dash.ventas.hoy}")
        }
        lastVentasHoy = dash.ventas.hoy

        if (prefs.notifyStock && dash.alertas.stockCritico > 0 && dash.alertas.stockCritico != lastStockCritico) {
            Notifier.notify("Stock crítico", "Productos críticos: ${dash.alertas.stockCritico}")
        }
        lastStockCritico = dash.alertas.stockCritico
    }

    private fun mockExtintoresVencimiento(count: Int): List<ExtintorVencimiento> {
        if (count <= 0) return emptyList()
        val max = kotlin.math.min(count, 10)
        return (1..max).map { i ->
            val dias = (i * 5) - 20 // produce valores negativos y <=30
            ExtintorVencimiento(
                id = i,
                codigo = "EXT-${1000 + i}",
                cliente = "Cliente A",
                sede = if (i % 2 == 0) "Sede A1" else "Sede A2",
                dias = dias,
                color = when {
                    dias <= 0 -> "rojo"
                    dias <= 30 -> "amarillo"
                    else -> "verde"
                }
            )
        }
    }

    fun refreshAlertas() {
        viewModelScope.launch {
            _state.value = _state.value.copy(alertasLoading = true, alertasError = null)
            try {
                val alertas = api.listarAlertasPendientes()
                _state.value = _state.value.copy(alertasLoading = false, alertasPendientes = alertas)
            } catch (e: Exception) {
                _state.value = _state.value.copy(alertasLoading = false, alertasError = e.message)
            }
        }
    }

    fun generarAlertasVenc() {
        viewModelScope.launch {
            api.generarAlertasVenc()
            refreshAlertas()
        }
    }

    fun generarAlertasStock() {
        viewModelScope.launch {
            api.generarAlertasStock()
            refreshAlertas()
        }
    }

    fun reenviarAlertas() {
        viewModelScope.launch {
            api.reenviarAlertas()
            refreshAlertas()
        }
    }

    fun agendarVisita(extintorId: Int) {
        // Mock: simplemente imprimir y quizá marcar color a amarillo si estaba rojo
        println("Agendar visita para extintor $extintorId")
        val current = _state.value.extintoresVencimientoLista
        val updated = current.map { e ->
            if (e.id == extintorId) e.copy(color = if (e.color == "rojo") "amarillo" else e.color) else e
        }
        _state.value = _state.value.copy(extintoresVencimientoLista = updated)
    }
}
