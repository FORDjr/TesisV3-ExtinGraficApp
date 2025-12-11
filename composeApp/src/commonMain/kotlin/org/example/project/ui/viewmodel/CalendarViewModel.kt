package org.example.project.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.example.project.data.api.CalendarApiService
import org.example.project.data.model.CalendarEvent
import org.example.project.data.model.CalendarDay
import org.example.project.data.model.CalendarUtils
import org.example.project.data.model.YearMonth
import org.example.project.utils.NotificationPreferences
import org.example.project.utils.Notifier

data class CalendarUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val events: List<CalendarEvent> = emptyList(),
    val currentMonth: YearMonth = run {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        YearMonth(now.year, now.monthNumber)
    },
    val days: List<CalendarDay> = emptyList(),
    val selectedDate: String? = null, // ISO yyyy-MM-dd
    val eventsForSelected: List<CalendarEvent> = emptyList(),
    val lastActionMessage: String? = null
)

class CalendarViewModel(
    private val api: CalendarApiService = CalendarApiService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(isLoading = true))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()
    private val notifiedEvents = mutableSetOf<Int>()

    init {
        cargarEventos()
    }

    fun cargarEventos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, lastActionMessage = null)
            try {
                val eventos = api.obtenerEventosCalendario()
                triggerNotifications(eventos)
                recomputarDias(eventos = eventos, month = _uiState.value.currentMonth, selected = _uiState.value.selectedDate)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Error desconocido")
            }
        }
    }

    private fun recomputarDias(eventos: List<CalendarEvent>, month: YearMonth, selected: String?) {
        val days = CalendarUtils.monthDays(month, eventos)
        val evSelected = selected?.let { s ->
            val target = s.take(10)
            eventos.filter { it.date.startsWith(target) }
        } ?: emptyList()
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            events = eventos,
            currentMonth = month,
            days = days,
            selectedDate = selected,
            eventsForSelected = evSelected
        )
    }

    fun previousMonth() {
        val st = _uiState.value
        val newMonth = st.currentMonth.previous()
        recomputarDias(st.events, newMonth, null)
    }

    fun nextMonth() {
        val st = _uiState.value
        val newMonth = st.currentMonth.next()
        recomputarDias(st.events, newMonth, null)
    }

    fun selectDay(day: CalendarDay) {
        val iso = day.date.toString()
        recomputarDias(_uiState.value.events, _uiState.value.currentMonth, iso)
    }

    fun marcarAlertaAtendida(alertaId: Int) {
        viewModelScope.launch {
            val ok = api.marcarAlertaAtendida(alertaId)
            if (ok) {
                _uiState.value = _uiState.value.copy(lastActionMessage = "Alerta marcada como atendida")
                cargarEventos()
            } else {
                _uiState.value = _uiState.value.copy(lastActionMessage = "No se pudo marcar la alerta")
            }
        }
    }

    private fun triggerNotifications(eventos: List<CalendarEvent>) {
        val prefs = NotificationPreferences.settings.value
        if (!prefs.notifyReminders) return
        eventos.filter { it.type.equals("EXTINTOR", ignoreCase = true) }
            .filter { val dias = it.daysToExpire ?: Long.MAX_VALUE; dias in 0..6 }
            .forEach { ev ->
                if (notifiedEvents.add(ev.id)) {
                    val title = "Extintor por vencer"
                    val message = "${ev.codigo ?: ev.title} vence el ${ev.date}"
                    Notifier.notify(title, message)
                }
            }
    }

    fun recalcularExtintores() {
        viewModelScope.launch {
            val ok = api.recalcularExtintores()
            if (ok) {
                _uiState.value = _uiState.value.copy(lastActionMessage = "Estados recalculados")
                cargarEventos()
            } else {
                _uiState.value = _uiState.value.copy(lastActionMessage = "No se pudo recalcular")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        api.close()
    }
}
