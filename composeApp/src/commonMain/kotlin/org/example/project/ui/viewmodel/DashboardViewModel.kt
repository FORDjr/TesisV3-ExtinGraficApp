package org.example.project.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.api.DashboardApiService
import org.example.project.data.model.DashboardUiState
import org.example.project.data.model.ExtintorVencimiento

class DashboardViewModel(
    private val api: DashboardApiService = DashboardApiService()
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState(loading = true))
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    init { refreshAll() }

    fun refreshAll(clienteId: Int? = null, sedeId: Int? = null) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val dash = api.fetchDashboard(clienteId, sedeId)
                val alertas = api.listarAlertasPendientes()
                val extList = mockExtintoresVencimiento(dash.inventario.extintores.vencen30)
                _state.value = _state.value.copy(
                    loading = false,
                    data = dash,
                    alertasPendientes = alertas,
                    alertasLoading = false,
                    error = null,
                    serviciosHoyAgendados = 0, // TODO real
                    serviciosHoyPendientes = 0, // TODO real
                    extintoresVencimientoLista = extList
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error")
            }
        }
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
