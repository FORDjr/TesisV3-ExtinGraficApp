package org.example.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.example.project.data.model.EstadoAprobacionMovimiento
import org.example.project.data.model.KardexFilters
import org.example.project.data.model.KardexResponse
import org.example.project.data.model.MovimientoInventario
import org.example.project.data.model.ProductoUI
import org.example.project.data.model.TipoMovimientoInventario
import org.example.project.ui.components.ButtonVariant
import org.example.project.ui.components.ExtintorButton
import org.example.project.ui.components.ExtintorCard
import org.example.project.ui.components.ExtintorChip
import org.example.project.ui.components.ExtintorTextField
import org.example.project.ui.components.StatusBadge
import org.example.project.ui.components.StatusType
import org.example.project.ui.theme.ExtintorColors
import org.example.project.ui.viewmodel.KardexViewModel

@Composable
fun KardexScreen(
    refreshSignal: Int = 0,
    viewModel: KardexViewModel = remember { KardexViewModel() }
) {
    val filtros by viewModel.filtros.collectAsState()
    val kardex by viewModel.kardex.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val productos by viewModel.productos.collectAsState()
    val productoBusqueda by viewModel.productoBusqueda.collectAsState()
    val productoSeleccionado by viewModel.productoSeleccionado.collectAsState()
    val exportLinks by viewModel.exportLinks.collectAsState()
    val uriHandler = LocalUriHandler.current
    var showAjusteDialog by remember { mutableStateOf(false) }
    var ajusteCantidad by remember { mutableStateOf("") }
    var ajusteMotivo by remember { mutableStateOf("") }
    var ajusteObs by remember { mutableStateOf("") }
    var ajusteError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0 && filtros.productoId != null) {
            viewModel.cargarKardex()
        }
    }

    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        state = listState
    ) {
        item {
            Column {
                Text(
                    text = "Kardex y movimientos",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Filtra por producto, tipo y estado. Exporta PDF/CSV y aprueba ajustes pendientes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            FiltrosKardexCard(
                productos = productos,
                productoBusqueda = productoBusqueda,
                filtros = filtros,
                productoSeleccionado = productoSeleccionado,
                isLoading = isLoading,
                onProductoSearch = viewModel::actualizarBusquedaProducto,
                onProductoSelect = viewModel::seleccionarProducto,
                onProductoIdChange = viewModel::actualizarProductoIdManual,
                onTipoChange = viewModel::updateTipo,
                onEstadoChange = viewModel::updateEstado,
                onDesdeChange = viewModel::updateDesde,
                onHastaChange = viewModel::updateHasta,
                onNuevoAjuste = {
                    ajusteError = null
                    showAjusteDialog = true
                },
                onConsultar = viewModel::cargarKardex,
                onExportPdf = {
                    viewModel.generarEnlacesExport { links -> uriHandler.openUri(links.pdf) }
                },
                onExportCsv = {
                    viewModel.generarEnlacesExport { links -> uriHandler.openUri(links.csv) }
                }
            )
        }

        if (kardex != null) {
            item { ResumenKardexCard(kardex!!) }
        }

        if (exportLinks != null) {
            item {
                ExtintorCard(elevated = false) {
                    Text(
                        text = "Enlaces de exportación listos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "CSV: ${exportLinks!!.csv}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "PDF: ${exportLinks!!.pdf}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (error != null) {
            item { ErrorBanner(message = error ?: "") }
        }

        if (isLoading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = ExtintorColors.ExtintorRed
                )
            }
        }

        kardex?.movimientos?.takeIf { it.isNotEmpty() }?.let { movimientos ->
            items(movimientos) { movimiento ->
                MovimientoCard(
                    movimiento = movimiento,
                    onAprobar = { viewModel.aprobarMovimiento(movimiento.id, true) },
                    onRechazar = { viewModel.aprobarMovimiento(movimiento.id, false) }
                )
            }
        } ?: item {
            if (kardex == null) {
                ExtintorCard(elevated = false) {
                    Text(
                        text = "Selecciona un producto para ver su kardex",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (showAjusteDialog) {
        AlertDialog(
            onDismissRequest = { showAjusteDialog = false },
            title = { Text("Nuevo ajuste pendiente") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = ajusteCantidad,
                        onValueChange = { ajusteCantidad = it },
                        label = { Text("Cantidad (puede ser negativa)") }
                    )
                    OutlinedTextField(
                        value = ajusteMotivo,
                        onValueChange = { ajusteMotivo = it },
                        label = { Text("Motivo") }
                    )
                    OutlinedTextField(
                        value = ajusteObs,
                        onValueChange = { ajusteObs = it },
                        label = { Text("Observaciones (opcional)") }
                    )
                    ajusteError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cantidad = ajusteCantidad.toIntOrNull()
                    if (cantidad == null || cantidad == 0) {
                        ajusteError = "Ingresa una cantidad distinta de 0"
                        return@TextButton
                    }
                    if (filtros.productoId == null) {
                        ajusteError = "Selecciona un producto"
                        return@TextButton
                    }
                    viewModel.crearAjustePendiente(
                        cantidad = cantidad,
                        motivo = ajusteMotivo,
                        observaciones = ajusteObs
                    ) { ok ->
                        if (ok) {
                            showAjusteDialog = false
                            ajusteCantidad = ""
                            ajusteMotivo = ""
                            ajusteObs = ""
                            ajusteError = null
                        } else {
                            ajusteError = "No se pudo crear el ajuste"
                        }
                    }
                }) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = { showAjusteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun FiltrosKardexCard(
    productos: List<ProductoUI>,
    productoBusqueda: String,
    filtros: KardexFilters,
    productoSeleccionado: ProductoUI?,
    isLoading: Boolean,
    onProductoSearch: (String) -> Unit,
    onProductoSelect: (ProductoUI?) -> Unit,
    onProductoIdChange: (String) -> Unit,
    onTipoChange: (TipoMovimientoInventario?) -> Unit,
    onEstadoChange: (EstadoAprobacionMovimiento?) -> Unit,
    onDesdeChange: (String) -> Unit,
    onHastaChange: (String) -> Unit,
    onNuevoAjuste: () -> Unit,
    onConsultar: () -> Unit,
    onExportPdf: () -> Unit,
    onExportCsv: () -> Unit
) {
    var showProducts by remember { mutableStateOf(false) }

    ExtintorCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Filtros",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Box {
                ExtintorTextField(
                    value = productoBusqueda,
                    onValueChange = {
                        onProductoSearch(it)
                        showProducts = true
                    },
                    label = "Producto o código",
                    placeholder = "Buscar por nombre o código",
                    leadingIcon = Icons.Default.Search
                )
                DropdownMenu(
                    expanded = showProducts && productos.isNotEmpty(),
                    onDismissRequest = { showProducts = false }
                ) {
                    productos.take(8).forEach { producto ->
                        DropdownMenuItem(
                            text = { Text("${producto.nombre} (${producto.codigo})") },
                            onClick = {
                                onProductoSelect(producto)
                                onProductoSearch("${producto.nombre} (${producto.codigo})")
                                showProducts = false
                            }
                        )
                    }
                }
            }
            ExtintorTextField(
                value = filtros.productoId?.toString().orEmpty(),
                onValueChange = { onProductoIdChange(it) },
                label = "ID de producto (manual)",
                placeholder = "Ej: 101",
                leadingIcon = null
            )
            productoSeleccionado?.let {
                Text(
                    text = "Seleccionado: ${it.nombre} (${it.codigo})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Tipo de movimiento", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf<Pair<String, TipoMovimientoInventario?>>(
                        "Todos" to null,
                        "Entradas" to TipoMovimientoInventario.ENTRADA,
                        "Salidas" to TipoMovimientoInventario.SALIDA,
                        "Ajustes" to TipoMovimientoInventario.AJUSTE
                    ).forEach { (label, tipo) ->
                        ExtintorChip(
                            text = label,
                            selected = filtros.tipo == tipo || (tipo == null && filtros.tipo == null),
                            onClick = { onTipoChange(tipo) }
                        )
                    }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Estado de aprobación", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    listOf<Pair<String, EstadoAprobacionMovimiento?>>(
                        "Todos" to null,
                        "Pendientes" to EstadoAprobacionMovimiento.PENDIENTE,
                        "Aprobados" to EstadoAprobacionMovimiento.APROBADO,
                        "Rechazados" to EstadoAprobacionMovimiento.RECHAZADO
                    ).forEach { (label, estado) ->
                        ExtintorChip(
                            text = label,
                            selected = filtros.estado == estado || (estado == null && filtros.estado == null),
                            onClick = { onEstadoChange(estado) }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateSelector(
                    label = "Desde",
                    value = filtros.desde.orEmpty(),
                    onSelect = onDesdeChange,
                    modifier = Modifier.weight(1f)
                )
                DateSelector(
                    label = "Hasta",
                    value = filtros.hasta.orEmpty(),
                    onSelect = onHastaChange,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExtintorButton(
                    text = "Ajuste",
                    onClick = onNuevoAjuste,
                    icon = Icons.Default.Add,
                    variant = ButtonVariant.Secondary,
                    enabled = filtros.productoId != null && !isLoading
                )
                ExtintorButton(
                    text = "Consultar",
                    onClick = onConsultar,
                    enabled = !isLoading
                )
                ExtintorButton(
                    text = "PDF",
                    onClick = onExportPdf,
                    icon = Icons.Default.FileDownload,
                    variant = ButtonVariant.Outline,
                    enabled = !isLoading
                )
                ExtintorButton(
                    text = "CSV",
                    onClick = onExportCsv,
                    icon = Icons.Default.FileDownload,
                    variant = ButtonVariant.Outline,
                    enabled = !isLoading
                )
            }
        }
    }
}

@Composable
private fun ResumenKardexCard(kardex: KardexResponse) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Resumen de ${kardex.producto.nombre} (${kardex.producto.codigo})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            val min = 110.dp
            ResumenCard("Entradas", kardex.totalEntradas.toString(), modifier = Modifier.width(min))
            ResumenCard("Salidas", kardex.totalSalidas.toString(), modifier = Modifier.width(min))
            ResumenCard("Ajustes", kardex.totalAjustes.toString(), modifier = Modifier.width(min))
            ResumenCard("Pendientes", kardex.pendientes.toString(), modifier = Modifier.width(min))
            ResumenCard("Stock actual", kardex.producto.stockActual.toString(), modifier = Modifier.width(min))
        }
    }
}

@Composable
private fun ResumenCard(title: String, value: String, modifier: Modifier = Modifier) {
    ExtintorCard(modifier = modifier, elevated = true) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MovimientoCard(
    movimiento: MovimientoInventario,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    ExtintorCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = movimiento.tipo.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Cantidad: ${movimiento.cantidad}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(
                    text = movimiento.estadoAprobacion.name.lowercase().replaceFirstChar { it.uppercase() },
                    status = when (movimiento.estadoAprobacion) {
                        EstadoAprobacionMovimiento.APROBADO -> StatusType.Success
                        EstadoAprobacionMovimiento.PENDIENTE -> StatusType.Warning
                        EstadoAprobacionMovimiento.RECHAZADO -> StatusType.Error
                    }
                )
            }
            movimiento.motivo?.takeIf { it.isNotBlank() }?.let {
                Text("Motivo: $it", style = MaterialTheme.typography.bodySmall)
            }
            movimiento.documento?.takeIf { it.isNotBlank() }?.let {
                Text("Documento: $it", style = MaterialTheme.typography.bodySmall)
            }
            movimiento.observaciones?.takeIf { it.isNotBlank() }?.let {
                Text("Obs: $it", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = "Fecha: ${movimiento.fechaRegistro}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (movimiento.estadoAprobacion == EstadoAprobacionMovimiento.PENDIENTE) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExtintorButton(
                        text = "Aprobar",
                        onClick = onAprobar,
                        icon = Icons.Default.Check
                    )
                    ExtintorButton(
                        text = "Rechazar",
                        onClick = onRechazar,
                        icon = Icons.Default.Close,
                        variant = ButtonVariant.Outline
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    ExtintorCard(elevated = false) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DateSelector(
    label: String,
    value: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val presets = remember {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val weekAgo = today.minus(DatePeriod(days = 7))
        val monthAgo = today.minus(DatePeriod(days = 30))
        val monthStart = kotlinx.datetime.LocalDate(today.year, today.monthNumber, 1)
        listOf(
            "Hoy" to today.toString(),
            "Hace 7 días" to weekAgo.toString(),
            "Hace 30 días" to monthAgo.toString(),
            "Inicio del mes" to monthStart.toString(),
            "Limpiar" to ""
        )
    }
    Column(modifier = modifier) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        ExtintorButton(
            text = if (value.isBlank()) "Elegir fecha" else value,
            onClick = { expanded = true },
            icon = Icons.Default.Today,
            modifier = Modifier.fillMaxWidth(),
            variant = ButtonVariant.Secondary
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            presets.forEach { (labelPreset, dateValue) ->
                DropdownMenuItem(
                    text = { Text(labelPreset) },
                    onClick = {
                        expanded = false
                        onSelect(dateValue)
                    }
                )
            }
        }
    }
}
