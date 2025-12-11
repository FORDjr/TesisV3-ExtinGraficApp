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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    onOpenExtintor: (Int?, String?) -> Unit = { _, _ -> },
    viewModel: CalendarViewModel = remember { CalendarViewModel() }
) {
    val state by viewModel.uiState.collectAsState()
    var selectedEvent by remember { mutableStateOf<DisplayEvent?>(null) }

    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0) {
            viewModel.cargarEventos()
        }
    }

    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val upcomingEvents = remember(state.events) {
        state.events.mapNotNull { it.toDisplayEvent(today) }
            .filter { it.daysFromToday in 0..30 } // limitar a un mes
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
            text = "Agenda y vencimientos",
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
                        CalendarEventCard(display, onClick = { selectedEvent = display })
                    }
                }
            }
        }
        state.lastActionMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

    selectedEvent?.let { display ->
        EventDetailDialog(
            display = display,
            onDismiss = { selectedEvent = null },
            onOpenExtintor = {
                onOpenExtintor(display.event.referenceId, display.event.codigo)
                selectedEvent = null
            },
            onMarkAlert = { alertId ->
                viewModel.marcarAlertaAtendida(alertId)
                selectedEvent = null
            },
            onRecalcular = {
                viewModel.recalcularExtintores()
                selectedEvent = null
            }
        )
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
                    text = "$count ${if (count == 1) "evento" else "eventos"}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Visitas y certificados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CalendarEventCard(display: DisplayEvent, onClick: () -> Unit) {
    val label = display.relativeLabel()
    val location = listOfNotNull(display.event.cliente, display.event.sede).joinToString(" / ").takeIf { it.isNotBlank() }

    ExtintorCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
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
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusBadge(
                text = display.event.typeLabel(),
                color = MaterialTheme.colorScheme.secondary
            )
            display.event.estado?.let {
                StatusBadge(
                    text = formatEstado(it),
                    color = when (it.uppercase()) {
                        "POR_VENCER", "PENDIENTE" -> MaterialTheme.colorScheme.error
                        "VENCIDO" -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        location?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        display.event.descripcion?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EventDetailDialog(
    display: DisplayEvent,
    onDismiss: () -> Unit,
    onOpenExtintor: () -> Unit,
    onMarkAlert: (Int) -> Unit,
    onRecalcular: () -> Unit
) {
    val event = display.event
    val isExtintor = event.type.equals("EXTINTOR", ignoreCase = true)
    val isAlerta = event.type.equals("ALERTA", ignoreCase = true)
    val canOpenExtintor = event.referenceId != null && (isExtintor || isAlerta)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(event.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                event.descripcion?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                val location = listOfNotNull(event.cliente, event.sede).joinToString(" / ").takeIf { it.isNotBlank() }
                location?.let {
                    Text("Cliente/Sede: $it", style = MaterialTheme.typography.bodySmall)
                }
                Text("Fecha: ${event.date}", style = MaterialTheme.typography.bodySmall)
                event.estado?.let { Text("Estado: ${formatEstado(it)}", style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canOpenExtintor) {
                    OutlinedButton(onClick = onOpenExtintor) { Text("Abrir ficha") }
                }
                when {
                    isAlerta && event.alertaId != null -> Button(onClick = { onMarkAlert(event.alertaId) }) { Text("Marcar atendida") }
                    isExtintor -> Button(onClick = onOpenExtintor) { Text("Ver detalle") }
                    else -> Button(onClick = onDismiss) { Text("Cerrar") }
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isExtintor) {
                    TextButton(onClick = onRecalcular) { Text("Recalcular") }
                }
                TextButton(onClick = onDismiss) { Text("Cerrar") }
            }
        }
    )
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
    1 -> "Mañana"
    else -> "En $daysFromToday días"
}

private fun CalendarEvent.toDisplayEvent(today: LocalDate): DisplayEvent? {
    val eventDate = this.localDate() ?: return null
    val diff = today.daysUntil(eventDate)
    return DisplayEvent(
        event = this,
        eventDate = eventDate,
        daysFromToday = diff,
        timeText = null
    )
}

private fun CalendarEvent.typeLabel(): String = when (type.uppercase()) {
    "ORDEN" -> "Orden de servicio"
    "ALERTA" -> "Alerta"
    else -> "Extintor"
}

private fun formatEstado(raw: String): String =
    raw.lowercase().replace('_', ' ').replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

@Composable
private fun StatusBadge(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold
    )
}

private fun LocalDate.monthNumberCompat(): Int = this.month.ordinal + 1
