package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import org.example.project.data.model.DashboardFilters
import org.example.project.data.model.DashboardPreset
import org.example.project.data.model.DashboardScope
import org.example.project.data.model.DashboardSerieValor
import org.example.project.data.model.DashboardTopProducto
import org.example.project.data.model.DashboardUiState
import org.example.project.data.model.DashboardVentasBlock
import org.example.project.ui.components.ExtintorCard
import org.example.project.ui.components.ExtintorChip
import org.example.project.ui.components.ExtintorTextField
import org.example.project.ui.viewmodel.DashboardViewModel
import org.example.project.utils.Formatters.formatPesos

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = androidx.compose.runtime.remember { DashboardViewModel() },
    onNavigateToVencimientos: () -> Unit = {},
    onNavigateToStockCritico: () -> Unit = {},
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Filtra por rango y cliente/sede, revisa top ventas y atiende alertas desde un solo lugar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Resumen primero
        item { SummarySection(state) }

        // Tendencias de ventas
        state.data?.ventas?.let { ventas ->
            item { VentasInsightSection(ventas) }
            if (ventas.topProductos.isNotEmpty()) {
                item { TopProductosSection(ventas.topProductos) }
            }
        }

        // Filtros rapidos despues de ver las metricas
        item {
            DashboardFiltersCard(
                filtros = state.filtros,
                preset = state.preset,
                scope = state.data?.scope,
                onPresetChange = viewModel::applyPreset,
                onClienteChange = viewModel::updateClienteFiltro,
                onSedeChange = viewModel::updateSedeFiltro,
                onDesdeChange = viewModel::updateDesdeFiltro,
                onHastaChange = viewModel::updateHastaFiltro,
                onApply = viewModel::aplicarFiltrosManuales
            )
        }

        // Acciones rapidas
        item {
            val quickActions: List<QuickAction> = listOf(
                QuickAction(
                    title = "Proximos vencimientos",
                    subtitle = "Agenda visitas y certificados",
                    icon = Icons.Filled.Event,
                    highlight = true,
                    onClick = onNavigateToVencimientos
                ),
                QuickAction(
                    title = "Alertas de stock",
                    subtitle = "Ver productos con stock critico",
                    icon = Icons.Filled.Warning,
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = onNavigateToStockCritico
                )
            )
            QuickActionsSection(actions = quickActions)
        }

    }
}

@Composable
private fun DashboardFiltersCard(
    filtros: DashboardFilters,
    preset: DashboardPreset,
    scope: DashboardScope?,
    onPresetChange: (DashboardPreset) -> Unit,
    onClienteChange: (String) -> Unit,
    onSedeChange: (String) -> Unit,
    onDesdeChange: (String) -> Unit,
    onHastaChange: (String) -> Unit,
    onApply: () -> Unit
) {
    ExtintorCard(elevated = false) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Filtros rapidos",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Ajusta rango y scope (cliente/sede) para el resumen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Filled.FilterAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExtintorChip(
                    text = "Hoy",
                    selected = preset == DashboardPreset.HOY,
                    onClick = { onPresetChange(DashboardPreset.HOY) }
                )
                ExtintorChip(
                    text = "Ultimos 7 dias",
                    selected = preset == DashboardPreset.ULTIMOS_7,
                    onClick = { onPresetChange(DashboardPreset.ULTIMOS_7) }
                )
                ExtintorChip(
                    text = "Mes actual",
                    selected = preset == DashboardPreset.MES_ACTUAL,
                    onClick = { onPresetChange(DashboardPreset.MES_ACTUAL) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExtintorTextField(
                    value = filtros.clienteId,
                    onValueChange = onClienteChange,
                    label = "Cliente ID",
                    placeholder = "Opcional",
                    leadingIcon = null,
                    modifier = Modifier.weight(1f)
                )
                ExtintorTextField(
                    value = filtros.sedeId,
                    onValueChange = onSedeChange,
                    label = "Sede ID",
                    placeholder = "Opcional",
                    leadingIcon = null,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DatePickerField(
                    label = "Desde",
                    value = filtros.desde,
                    onDateSelected = onDesdeChange,
                    modifier = Modifier.weight(1f)
                )
                DatePickerField(
                    label = "Hasta",
                    value = filtros.hasta,
                    onDateSelected = onHastaChange,
                    modifier = Modifier.weight(1f)
                )
            }

            val rangoLabel = scope?.let {
                val desde = it.desde ?: filtros.desde ?: "-"
                val hasta = it.hasta ?: filtros.hasta ?: "-"
                "Rango activo: $desde -> $hasta"
            } ?: "Rango activo: ${filtros.desde ?: "-"} -> ${filtros.hasta ?: "-"}"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rangoLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onApply, shape = CircleShape) {
                    Text("Aplicar")
                }
            }
        }
    }
}

@Composable
private fun SummarySection(state: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Resumen",
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
            else -> SummaryMetricsGrid(state)
        }
    }
}

@Composable
private fun SummaryMetricsGrid(state: DashboardUiState) {
    val ventas = state.data?.ventas
    val metrics: List<DashboardMetric> = listOf(
        DashboardMetric(
            title = "Ventas hoy",
            value = formatPesos(ventas?.hoy ?: 0L),
            trend = trendLabel(ventas?.crecimiento?.ventasHoyPct),
            positiveTrend = (ventas?.crecimiento?.ventasHoyPct ?: 0) >= 0
        ),
        DashboardMetric(
            title = "Ordenes hoy",
            value = (ventas?.ordenesHoy ?: 0).toString(),
            trend = trendLabel(ventas?.crecimiento?.ordenesPct),
            positiveTrend = (ventas?.crecimiento?.ordenesPct ?: 0) >= 0
        ),
        DashboardMetric(
            title = "Ticket promedio",
            value = formatPesos(ventas?.ticketPromedio ?: 0L),
            trend = trendLabel(ventas?.crecimiento?.ticketPct),
            positiveTrend = (ventas?.crecimiento?.ticketPct ?: 0) >= 0
        ),
        DashboardMetric(
            title = "Ventas mes",
            value = formatPesos(ventas?.mes ?: 0L),
            trend = trendLabel(ventas?.crecimiento?.mesPct),
            positiveTrend = (ventas?.crecimiento?.mesPct ?: 0) >= 0
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

private fun trendLabel(value: Int?): String {
    val v = value ?: 0
    return if (v > 0) "+${v}%" else "${v}%"
}

@Composable
private fun VentasInsightSection(ventas: DashboardVentasBlock) {
    ExtintorCard(elevated = false) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Tendencia de ventas",
                style = MaterialTheme.typography.titleMedium
            )
            if (ventas.serie.isEmpty()) {
                Text(
                    text = "Aun no hay ventas en el rango seleccionado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                VentasTrendCard(ventas.serie)
            }
        }
    }
}

@Composable
private fun VentasTrendCard(serie: List<DashboardSerieValor>) {
    val maxValue = serie.maxOfOrNull { it.valor }?.coerceAtLeast(1) ?: 1
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        serie.forEach { punto ->
            val factor = punto.valor.toFloat() / maxValue.toFloat()
            val barHeight = (120.dp * factor).coerceAtLeast(12.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.width(84.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(barHeight)
                        .width(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                )
                Text(
                    text = punto.label.takeLast(5),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = formatPesos(punto.valor),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TopProductosSection(productos: List<DashboardTopProducto>) {
    ExtintorCard(elevated = false) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Productos mas vendidos",
                style = MaterialTheme.typography.titleMedium
            )
            val top = productos.take(5)
            top.forEachIndexed { index, prod ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = prod.nombre,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Unidades: ${prod.cantidad}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = formatPesos(prod.monto),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (index < top.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerField(
    label: String,
    value: String?,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val initialDate = remember(value) {
        value?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today
    }
    val dateState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    )

    OutlinedTextField(
        value = value ?: "",
        onValueChange = {},
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        modifier = modifier
            .clickable { showPicker = true }
            .fillMaxWidth(),
        singleLine = true,
        readOnly = true
    )

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dateState.selectedDateMillis?.let { millis ->
                            val selected = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                            onDateSelected(selected.toString())
                        }
                        showPicker = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = dateState)
        }
    }
}

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
            text = "Acciones rapidas",
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
