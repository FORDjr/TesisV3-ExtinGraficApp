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
    val eventsForSelected: List<CalendarEvent> = emptyList()
)

class CalendarViewModel(
    private val api: CalendarApiService = CalendarApiService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState(isLoading = true))
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        cargarEventos()
    }

    fun cargarEventos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val eventos = api.obtenerEventosCalendario()
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

    override fun onCleared() {
        super.onCleared()
        api.close()
    }
}
