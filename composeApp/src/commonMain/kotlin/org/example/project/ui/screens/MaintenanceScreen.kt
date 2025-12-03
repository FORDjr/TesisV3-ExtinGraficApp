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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.example.project.data.auth.AuthManager
import org.example.project.data.maintenance.ExtinguisherAsset
import org.example.project.data.maintenance.ExtinguisherStatus
import org.example.project.data.maintenance.LoanRecord
import org.example.project.data.maintenance.LoanStatus
import org.example.project.data.maintenance.MaintenanceRecord
import org.example.project.data.maintenance.MaintenanceStatus
import org.example.project.data.maintenance.PartInventoryItem
import org.example.project.data.maintenance.StockAlert
import org.example.project.ui.components.ButtonVariant
import org.example.project.ui.components.ExtintorButton
import org.example.project.ui.components.ExtintorCard
import org.example.project.ui.components.ExtintorChip
import org.example.project.ui.components.StatusBadge
import org.example.project.ui.components.StatusType
import org.example.project.ui.viewmodel.MaintenanceOverview
import org.example.project.ui.viewmodel.MaintenanceViewModel

private enum class MaintenanceTab(val label: String) {
    WORKSHOP("Taller"),
    FIELD("Terreno"),
    LOANS("Prestamos"),
    QR("QR"),
    PARTS("Repuestos")
}

@Composable
fun MaintenanceScreen(
    viewModel: MaintenanceViewModel = remember { MaintenanceViewModel() },
    refreshSignal: Int = 0
) {
    val overview by viewModel.maintenanceOverview.collectAsState()
    val workshopRecords by viewModel.workshopMaintenances.collectAsState()
    val fieldRecords by viewModel.fieldMaintenances.collectAsState()
    val loanRecords by viewModel.loanRecords.collectAsState()
    val extinguisherAssets by viewModel.extinguishers.collectAsState()
    val authState by AuthManager.authState.collectAsState()

    var selectedTab by remember { mutableStateOf(MaintenanceTab.QR) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingExtinguisher by remember { mutableStateOf<ExtinguisherAsset?>(null) }
    val actorName = authState.userName.ifBlank { "Tecnico" }

    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0) {
            viewModel.refreshAnalytics()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "Resumen de Mantencion",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        MaintenanceSummaryGrid(overview)
        MaintenanceTabSelector(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
        when (selectedTab) {
            MaintenanceTab.WORKSHOP -> MaintenanceRecordList(
                title = "Servicios en taller",
                emptyMessage = "Sin mantenimientos en taller",
                records = workshopRecords
            )
            MaintenanceTab.FIELD -> MaintenanceRecordList(
                title = "Servicios en terreno",
                emptyMessage = "Sin servicios en terreno",
                records = fieldRecords
            )
            MaintenanceTab.LOANS -> LoanRecordList(
                loans = loanRecords,
                emptyMessage = "Sin prestamos activos"
            )
            MaintenanceTab.QR -> QrExtinguisherList(
                assets = extinguisherAssets,
                onReprint = { code ->
                    viewModel.reprintQr(code, actorName, "Reimpresion desde app")
                },
                onCreate = { showCreateDialog = true },
                onMove = { asset -> editingExtinguisher = asset }
            )
            MaintenanceTab.PARTS -> PartsSection(viewModel = viewModel)
        }
    }

    if (showCreateDialog) {
        CreateExtinguisherDialog(
            suggestedCode = "E${extinguisherAssets.size + 1}",
            onDismiss = { showCreateDialog = false },
            onCreate = { code, owner, location, status, notes, reportResult ->
                viewModel.createExtinguisher(code, owner, location, status, actorName, notes) { result ->
                    if (result.isSuccess) {
                        reportResult(null)
                        showCreateDialog = false
                    } else {
                        val message = result.exceptionOrNull()?.localizedMessage ?: "No se pudo crear el extintor"
                        reportResult(message)
                    }
                }
            }
        )
    }

    editingExtinguisher?.let { asset ->
        MoveExtinguisherDialog(
            asset = asset,
            onDismiss = { editingExtinguisher = null },
            onMove = { location, status, notes, reportResult ->
                viewModel.updateExtinguisherLocation(
                    code = asset.code,
                    newLocation = location,
                    actor = actorName,
                    status = status,
                    notes = notes
                ) { result ->
                    if (result.isSuccess) {
                        reportResult(null)
                        editingExtinguisher = null
                    } else {
                        val message = result.exceptionOrNull()?.localizedMessage ?: "No se pudo actualizar la ubicacion"
                        reportResult(message)
                    }
                }
            }
        )
    }
}

@Composable
private fun MaintenanceSummaryGrid(overview: MaintenanceOverview) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SummarySection(
            title = "Ubicacion de extintores",
            entries = listOf(
                "Total" to overview.totalExtinguishers,
                "Disponibles" to overview.availableExtinguishers,
                "Taller" to overview.workshopExtinguishers,
                "Terreno" to overview.fieldExtinguishers,
                "Prestamos" to overview.loanExtinguishers
            )
        )
        SummarySection(
            title = "Operaciones activas",
            entries = listOf(
                "Taller" to overview.activeWorkshop,
                "Terreno" to overview.activeField,
                "Prestamos" to overview.activeLoans,
                "Alertas" to overview.alerts
            )
        )
    }
}

@Composable
private fun SummarySection(title: String, entries: List<Pair<String, Int>>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        entries.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { (label, value) ->
                    ExtintorCard(
                        modifier = Modifier.weight(1f),
                        elevated = false
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = value.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MaintenanceTabSelector(selectedTab: MaintenanceTab, onTabSelected: (MaintenanceTab) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(MaintenanceTab.values()) { tab ->
            ExtintorChip(
                text = tab.label,
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

@Composable
private fun MaintenanceRecordList(
    title: String,
    emptyMessage: String,
    records: List<MaintenanceRecord>
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (records.isEmpty()) {
            EmptyState(emptyMessage)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                records.forEach { record ->
                    MaintenanceRecordCard(record = record)
                }
            }
        }
    }
}

@Composable
private fun MaintenanceRecordCard(record: MaintenanceRecord) {
    val (statusLabel, statusType) = record.status.toBadge()

    ExtintorCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Extintor ${record.extinguisherCode}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                record.client?.takeIf { it.isNotBlank() }?.let { client ->
                    Text(
                        text = "Cliente: $client",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                record.location?.takeIf { it.isNotBlank() }?.let { location ->
                    Text(
                        text = "Ubicacion: $location",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Registrado: ${formatDate(record.registeredOn)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                record.expectedDelivery?.let {
                    Text(
                        text = "Entrega estimada: ${formatDate(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            StatusBadge(text = statusLabel, status = statusType)
        }
    }
}

@Composable
private fun LoanRecordList(loans: List<LoanRecord>, emptyMessage: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Prestamos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (loans.isEmpty()) {
            EmptyState(emptyMessage)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                loans.forEach { loan ->
                    LoanRecordCard(loan = loan)
                }
            }
        }
    }
}

@Composable
private fun LoanRecordCard(loan: LoanRecord) {
    val (label, statusType) = loan.status.toBadge()

    ExtintorCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = loan.client,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tecnico: ${loan.technician}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Programado: ${formatDate(loan.scheduledAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                loan.expectedReturnDate?.let {
                    Text(
                        text = "Retorno estimado: ${formatDate(it)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val activeLoanExts = loan.loanExtinguishers.filter { !it.returned }
                if (activeLoanExts.isNotEmpty()) {
                    val activeCodes = activeLoanExts.joinToString { it.code }
                    Text(
                        text = "Extintores prestados: $activeCodes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val locations = activeLoanExts.mapNotNull { it.approxLocation }.distinct()
                    if (locations.isNotEmpty()) {
                        Text(
                            text = "Ubicacion aproximada: ${locations.joinToString()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            StatusBadge(text = label, status = statusType)
        }
    }
}

@Composable
private fun QrExtinguisherList(
    assets: List<ExtinguisherAsset>,
    onReprint: (String) -> Unit,
    onCreate: () -> Unit,
    onMove: (ExtinguisherAsset) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(ExtinguisherFilter.ALL) }
    val filtered = remember(assets, selectedFilter) {
        assets.filter { selectedFilter.accepts(it) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Extintores con QR",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${filtered.size} de ${assets.size} mostrando",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ExtintorButton(
                text = "Crear QR",
                icon = Icons.Filled.Add,
                variant = ButtonVariant.Primary,
                onClick = onCreate
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ExtinguisherFilter.values()) { filter ->
                ExtintorChip(
                    text = filter.label,
                    selected = filter == selectedFilter,
                    onClick = { selectedFilter = filter }
                )
            }
        }

        if (filtered.isEmpty()) {
            EmptyState("Sin extintores para este filtro")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                filtered.forEach { asset ->
                    QrExtinguisherCard(
                        asset = asset,
                        onReprint = { onReprint(asset.code) },
                        onMove = { onMove(asset) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QrExtinguisherCard(
    asset: ExtinguisherAsset,
    onReprint: () -> Unit,
    onMove: () -> Unit
) {
    val (statusLabel, statusType) = asset.status.toBadge()
    ExtintorCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = asset.code,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    StatusBadge(text = statusLabel, status = statusType)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Propietario: ${asset.owner}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Ubicacion: ${asset.location ?: "Sin ubicacion"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val lastRevision = asset.lastMaintenanceDate ?: asset.intakeDate
                Text(
                    text = "Ultima revision: ${formatDate(lastRevision)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExtintorButton(
                    text = "Mover",
                    variant = ButtonVariant.Secondary,
                    onClick = onMove
                )
                ExtintorButton(
                    text = "Reimprimir",
                    icon = Icons.Filled.QrCode,
                    variant = ButtonVariant.Outline,
                    onClick = onReprint
                )
            }
        }
    }
}

@Composable
private fun PartsSection(viewModel: MaintenanceViewModel) {
    val parts by viewModel.partInventory.collectAsState()
    val report by viewModel.monthlyReport.collectAsState()
    val suggestions by viewModel.purchaseSuggestions.collectAsState()
    val alerts by viewModel.stockAlerts.collectAsState()
    var addTarget by remember { mutableStateOf<PartInventoryItem?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Alertas de stock",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (alerts.isEmpty()) {
            EmptyState("Sin alertas activas")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                alerts.forEach { alert ->
                    StockAlertCard(alert = alert)
                }
            }
        }

        Text(
            text = "Inventario de repuestos",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (parts.isEmpty()) {
            EmptyState("Sin repuestos registrados")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                parts.forEach { part ->
                    PartCard(part = part, onAddStock = { addTarget = part })
                }
            }
        }

        Text(
            text = "Consumo mensual",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (report.isEmpty()) {
            EmptyState("Sin consumo registrado")
        } else {
            ExtintorCard(elevated = false) {
                report.forEach { item ->
                    Text(
                        text = "- : ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            text = "Sugerencias de compra",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (suggestions.isEmpty()) {
            EmptyState("Sin sugerencias disponibles")
        } else {
            ExtintorCard(elevated = false) {
                suggestions.forEach { suggestion ->
                    Text(
                        text = "- :  ()",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    addTarget?.let { part ->
        AddStockDialog(
            part = part,
            onDismiss = { addTarget = null },
            onConfirm = { quantity, notes ->
                viewModel.addPartStock(part.id, quantity, "Sistema", notes)
                addTarget = null
            }
        )
    }
}

@Composable
private fun StockAlertCard(alert: StockAlert) {
    ExtintorCard(modifier = Modifier.fillMaxWidth(), elevated = false) {
        Text(
            text = alert.partName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Stock actual:  (min )",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun PartCard(part: PartInventoryItem, onAddStock: () -> Unit) {
    ExtintorCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = part.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "ID: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Stock:  (min )",
            style = MaterialTheme.typography.bodySmall,
            color = if (part.isBelowMinimum) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        ExtintorButton(
            text = "Ingresar stock",
            onClick = onAddStock,
            variant = ButtonVariant.Secondary
        )
    }
}

@Composable
private fun AddStockDialog(
    part: PartInventoryItem,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, notes: String?) -> Unit
) {
    var quantity by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar stock a ") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Cantidad") }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") }
                )
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = quantity.toIntOrNull()
                if (parsed == null || parsed <= 0) {
                    error = "Cantidad invalida"
                } else {
                    onConfirm(parsed, notes.takeUnless { it.isBlank() })
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun CreateExtinguisherDialog(
    suggestedCode: String,
    onDismiss: () -> Unit,
    onCreate: (
        code: String,
        owner: String,
        location: String?,
        status: ExtinguisherStatus,
        notes: String?,
        reportResult: (String?) -> Unit
    ) -> Unit
) {
    var code by remember { mutableStateOf(suggestedCode) }
    var owner by remember { mutableStateOf("ExtinGrafic") }
    var location by remember { mutableStateOf("Taller") }
    var notes by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(ExtinguisherStatus.AVAILABLE) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear nuevo extintor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Numero / Codigo") },
                    supportingText = { Text("Sugerido: $suggestedCode") }
                )
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("Propietario / Cliente") }
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ubicacion inicial") }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") }
                )
                Text(
                    text = "Estado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExtinguisherStatusSelector(selected = status, onSelect = { status = it })
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val normalizedLocation = location.trim()
                if (normalizedLocation.isEmpty()) {
                    error = "Debes indicar una ubicacion"
                    return@Button
                }
                val normalizedCode = code.trim().ifBlank { suggestedCode }
                onCreate(
                    normalizedCode,
                    owner.trim().ifBlank { "ExtinGrafic" },
                    normalizedLocation,
                    status,
                    notes.trim().ifBlank { null }
                ) { result ->
                    error = result
                }
            }) {
                Text("Generar QR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun MoveExtinguisherDialog(
    asset: ExtinguisherAsset,
    onDismiss: () -> Unit,
    onMove: (
        location: String,
        status: ExtinguisherStatus,
        notes: String?,
        reportResult: (String?) -> Unit
    ) -> Unit
) {
    var location by remember { mutableStateOf(asset.location ?: "") }
    var notes by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(asset.status) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mover ${asset.code}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Nueva ubicacion") }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") }
                )
                Text(
                    text = "Estado",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExtinguisherStatusSelector(selected = status, onSelect = { status = it })
                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val normalizedLocation = location.trim()
                if (normalizedLocation.isEmpty()) {
                    error = "Debes indicar una ubicacion"
                    return@Button
                }
                onMove(normalizedLocation, status, notes.trim().ifBlank { null }) { result ->
                    error = result
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ExtinguisherStatusSelector(selected: ExtinguisherStatus, onSelect: (ExtinguisherStatus) -> Unit) {
    val options = listOf(
        ExtinguisherStatus.AVAILABLE,
        ExtinguisherStatus.IN_WORKSHOP,
        ExtinguisherStatus.IN_FIELD_SERVICE,
        ExtinguisherStatus.ON_LOAN,
        ExtinguisherStatus.OUT_OF_SERVICE
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(options) { option ->
            ExtintorChip(
                text = option.displayLabel(),
                selected = option == selected,
                onClick = { onSelect(option) }
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun MaintenanceStatus.toBadge(): Pair<String, StatusType> = when (this) {
    MaintenanceStatus.REGISTERED -> "Registrado" to StatusType.Info
    MaintenanceStatus.CHECK_IN -> "Ingreso" to StatusType.Info
    MaintenanceStatus.WAITING_PARTS -> "Esperando repuestos" to StatusType.Warning
    MaintenanceStatus.IN_PROGRESS -> "En progreso" to StatusType.Warning
    MaintenanceStatus.ON_LOAN -> "En prestamo" to StatusType.Warning
    MaintenanceStatus.COMPLETED -> "Completado" to StatusType.Success
    MaintenanceStatus.CANCELLED -> "Cancelado" to StatusType.Neutral
}

private fun LoanStatus.toBadge(): Pair<String, StatusType> = when (this) {
    LoanStatus.PREPARING -> "Preparando" to StatusType.Info
    LoanStatus.ACTIVE -> "Activo" to StatusType.Warning
    LoanStatus.RETURNED -> "Devuelto" to StatusType.Success
    LoanStatus.CANCELLED -> "Cancelado" to StatusType.Neutral
}

private fun ExtinguisherStatus.toBadge(): Pair<String, StatusType> = when (this) {
    ExtinguisherStatus.AVAILABLE -> "Disponible" to StatusType.Success
    ExtinguisherStatus.IN_WORKSHOP -> "Taller" to StatusType.Info
    ExtinguisherStatus.IN_FIELD_SERVICE -> "Terreno" to StatusType.Warning
    ExtinguisherStatus.ON_LOAN -> "Prestamo" to StatusType.Warning
    ExtinguisherStatus.OUT_OF_SERVICE -> "Fuera de servicio" to StatusType.Neutral
}

private fun ExtinguisherStatus.displayLabel(): String = when (this) {
    ExtinguisherStatus.AVAILABLE -> "Disponible"
    ExtinguisherStatus.IN_WORKSHOP -> "En taller"
    ExtinguisherStatus.IN_FIELD_SERVICE -> "En terreno"
    ExtinguisherStatus.ON_LOAN -> "Prestamo"
    ExtinguisherStatus.OUT_OF_SERVICE -> "Fuera de servicio"
}

private enum class ExtinguisherFilter(val label: String, private val statuses: Set<ExtinguisherStatus>?) {
    ALL("Todos", null),
    WORKSHOP("Taller", setOf(ExtinguisherStatus.IN_WORKSHOP)),
    FIELD("Terreno", setOf(ExtinguisherStatus.IN_FIELD_SERVICE)),
    LOAN("Prestamo", setOf(ExtinguisherStatus.ON_LOAN)),
    AVAILABLE("Disponibles", setOf(ExtinguisherStatus.AVAILABLE));

    fun accepts(asset: ExtinguisherAsset): Boolean = statuses?.contains(asset.status) ?: true
}

private fun formatDate(date: LocalDate?): String {
    if (date == null) return "Sin fecha"
    val month = date.month.ordinal + 1
    return "%02d/%02d/%04d".format(date.dayOfMonth, month, date.year)
}
