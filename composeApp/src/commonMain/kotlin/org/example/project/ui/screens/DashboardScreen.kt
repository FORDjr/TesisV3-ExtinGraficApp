package org.example.project.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.data.model.AlertaDto
import org.example.project.data.model.DashboardResponse
import org.example.project.data.model.DashboardUiState
import org.example.project.ui.components.ExtintorCard
import org.example.project.ui.viewmodel.DashboardViewModel
import kotlin.math.round
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import org.example.project.utils.Formatters.formatPesos

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = remember { DashboardViewModel() },
    onNavigateToVencimientos: () -> Unit = {},
    refreshSignal: Int = 0
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0) {
            viewModel.refreshAll()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                SummarySection(state)
            }
        }

        item {
            QuickActionsSection(
                actions = listOf(
                    QuickAction(
                        title = "Ver Agenda",
                        subtitle = "Extintores por vencer",
                        iconTint = MaterialTheme.colorScheme.error,
                        icon = Icons.Filled.Event,
                        highlight = true,
                        onClick = onNavigateToVencimientos
                    ),
                    QuickAction(
                        title = "Generar QR",
                        subtitle = "Descarga etiquetas",
                        icon = Icons.Filled.QrCode2,
                        onClick = { viewModel.generarAlertasVenc() }
                    ),
                    QuickAction(
                        title = "Ver Mapa",
                        subtitle = "Ubicación de clientes",
                        icon = Icons.Filled.Map,
                        onClick = { /* TODO: Navegar a mapa */ }
                    )
                )
            )
        }

        item {
            AlertasSection(
                state = state,
                onRefresh = { viewModel.refreshAlertas() }
            )
        }
    }
}

@Composable
private fun SummarySection(state: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Resumen de Hoy",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when {
            state.loading && state.data == null -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.data == null -> {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {
                SummaryMetricsGrid(state.data)
            }
        }
    }
}

@Composable
private fun SummaryMetricsGrid(data: DashboardResponse?) {
    val ventas = data?.ventas
    val metrics = listOf(
        DashboardMetric(
            title = "Ventas Hoy",
            value = formatPesos(ventas?.hoy ?: 0L),
            trend = trendLabel(ventas?.crecimiento?.ventasHoyPct),
            positiveTrend = (ventas?.crecimiento?.ventasHoyPct ?: 0) > 0
        ),
        DashboardMetric(
            title = "Órdenes Hoy",
            value = (ventas?.ordenesHoy ?: 0).toString(),
            trend = trendLabel(ventas?.crecimiento?.ordenesPct),
            positiveTrend = (ventas?.crecimiento?.ordenesPct ?: 0) > 0
        ),
        DashboardMetric(
            title = "Compra Prom",
            value = formatPesos(ventas?.ticketPromedio ?: 0L),
            trend = trendLabel(ventas?.crecimiento?.ticketPct),
            positiveTrend = (ventas?.crecimiento?.ticketPct ?: 0) > 0
        ),
        DashboardMetric(
            title = "Ventas Mes",
            value = formatPesos(ventas?.mes ?: 0L),
            trend = trendLabel(ventas?.crecimiento?.mesPct),
            positiveTrend = (ventas?.crecimiento?.mesPct ?: 0) > 0
        )
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        metrics.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { metric ->
                    DashboardMetricCard(
                        metric = metric,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun trendLabel(value: Int?): String = "${value ?: 0}%"

@Composable
private fun DashboardMetricCard(
    metric: DashboardMetric,
    modifier: Modifier = Modifier
) {
    ExtintorCard(
        modifier = modifier,
        elevated = true
    ) {
        Text(
            text = metric.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = metric.value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        metric.trend?.let { trendLabel ->
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val trendColor = if (metric.positiveTrend) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
                Icon(
                    imageVector = if (metric.positiveTrend) {
                        Icons.AutoMirrored.Filled.TrendingUp
                    } else {
                        Icons.AutoMirrored.Filled.TrendingDown
                    },
                    contentDescription = null,
                    tint = trendColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = trendLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = trendColor
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(actions: List<QuickAction>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Acciones Rápidas",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        actions.forEach { action ->
            ExtintorCard(
                elevated = false,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = if (action.highlight) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                tint = action.iconTint ?: MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = action.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (action.highlight) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            action.subtitle?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Button(onClick = action.onClick, shape = CircleShape) {
                        Text("Ir")
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertasSection(
    state: DashboardUiState,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Alertas Pendientes",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        when {
            state.alertasLoading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.alertasError != null -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = state.alertasError,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onRefresh) {
                        Text("Reintentar")
                    }
                }
            }
            state.alertasPendientes.isEmpty() -> {
                ExtintorCard(elevated = false) {
                    Text(
                        text = "Sin alertas pendientes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.alertasPendientes.take(3).forEach { alerta ->
                        AlertCard(alerta = alerta)
                    }
                    if (state.alertasPendientes.size > 3) {
                        Text(
                            text = "${state.alertasPendientes.size - 3} alertas adicionales…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(onClick = onRefresh) {
                        Text("Refrescar")
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alerta: AlertaDto) {
    ExtintorCard(elevated = false) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "#${alerta.id} • ${alerta.tipo}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Generada: ${alerta.fechaGenerada}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Enviada: ${if (alerta.enviada) "Sí" else "No"} • Reintentos: ${alerta.reintentos}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Immutable
private data class DashboardMetric(
    val title: String,
    val value: String,
    val trend: String?,
    val positiveTrend: Boolean
)

@Immutable
private data class QuickAction(
    val title: String,
    val subtitle: String?,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconTint: Color? = null,
    val highlight: Boolean = false,
    val onClick: () -> Unit
)
