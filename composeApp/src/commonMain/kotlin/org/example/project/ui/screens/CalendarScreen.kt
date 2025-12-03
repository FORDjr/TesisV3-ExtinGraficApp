package org.example.project.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.example.project.data.model.CalendarEvent
import org.example.project.ui.components.ExtintorCard
import org.example.project.ui.viewmodel.CalendarViewModel
import kotlin.math.abs

@Composable
fun CalendarScreen(
    refreshSignal: Int = 0,
    viewModel: CalendarViewModel = remember { CalendarViewModel() }
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0) {
            viewModel.cargarEventos()
        }
    }

    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val upcomingEvents = remember(state.events) {
        state.events.mapNotNull { it.toDisplayEvent(today) }
            .filter { it.daysFromToday >= 0 }
            .sortedBy { it.eventDate }
    }
    val nextSevenCount = upcomingEvents.count { it.daysFromToday in 0..6 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        UpcomingSummaryCard(count = nextSevenCount)

        Text(
            text = "Eventos de hoy",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Text(
                    text = state.error ?: "Error",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            upcomingEvents.isEmpty() -> {
                EmptyCalendarState(message = "Sin eventos proximos")
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(upcomingEvents, key = { it.event.id }) { display ->
                        CalendarEventCard(display)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingSummaryCard(count: Int) {
    ExtintorCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Proximos 7 dias",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = " eventos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Proximos",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CalendarEventCard(display: DisplayEvent) {
    val label = display.relativeLabel()
    val timeText = display.timeText
    val extra = display.event.daysToExpire?.let {
        when {
            it > 0 -> "Faltan  dias"
            it == 0L -> "Vence hoy"
            else -> "Vencido hace  dias"
        }
    }

    ExtintorCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = display.event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        extra?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        timeText?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyCalendarState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class DisplayEvent(
    val event: CalendarEvent,
    val eventDate: LocalDate,
    val daysFromToday: Int,
    val timeText: String?
)

private fun DisplayEvent.relativeLabel(): String = when (daysFromToday) {
    0 -> "Hoy"
    1 -> "Manana"
    else -> "En  dias"
}

private fun CalendarEvent.toDisplayEvent(today: LocalDate): DisplayEvent? {
    val eventDate = this.localDate() ?: return null
    val diff = today.daysUntil(eventDate)
    val timeText = localDateTimeText()
    return DisplayEvent(
        event = this,
        eventDate = eventDate,
        daysFromToday = diff,
        timeText = timeText
    )
}

private fun CalendarEvent.localDateTimeText(): String? {
    val parsed: LocalDateTime? = rawDateTime
        ?.takeIf { it.length >= 19 }
        ?.let { runCatching { LocalDateTime.parse(it.substring(0, 19)) }.getOrNull() }

    val fallback = localDate()?.let { LocalDateTime(it.year, it.monthNumberCompat(), it.dayOfMonth, 0, 0) }
    val effective = parsed ?: fallback
    return effective?.let { "%02d:%02d".format(it.hour, it.minute) }?.takeIf { it != "00:00" }
}

private fun LocalDate.monthNumberCompat(): Int = this.month.ordinal + 1