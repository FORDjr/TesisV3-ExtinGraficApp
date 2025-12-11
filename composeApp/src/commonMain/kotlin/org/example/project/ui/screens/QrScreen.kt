package org.example.project.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import org.example.project.data.auth.AuthManager
import org.example.project.data.maintenance.ExtinguisherAsset
import org.example.project.data.maintenance.ExtinguisherStatus
import org.example.project.ui.components.ButtonVariant
import org.example.project.ui.components.ExtintorButton
import org.example.project.ui.components.ExtintorCard
import org.example.project.ui.components.ExtintorChip
import org.example.project.ui.viewmodel.MaintenanceViewModel
import org.example.project.ui.viewmodel.QrUiState

private enum class QrSection(val label: String) { SERVICES("Servicios"), EXTINTORES("Extintores") }

@Composable
fun QrScreen(
    refreshSignal: Int = 0,
    viewModel: MaintenanceViewModel = remember { MaintenanceViewModel() }
) {
    val qrState by viewModel.qrState.collectAsState()
    val extinguisherAssets by viewModel.extinguishers.collectAsState()
    val authState by AuthManager.authState.collectAsState()
    var qrSection by remember { mutableStateOf(QrSection.SERVICES) }
    var showCreate by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ExtinguisherAsset?>(null) }

    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0) viewModel.refreshAnalytics()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("QR y Servicios", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QrSection.values().forEach { section ->
                ExtintorChip(
                    text = section.label,
                    selected = qrSection == section,
                    onClick = { qrSection = section }
                )
            }
        }

        when (qrSection) {
            QrSection.SERVICES -> QrServicesPanel(
                qrState = qrState,
                extintores = extinguisherAssets,
                onScan = { viewModel.buscarExtintorPorQr(it) },
                onRegister = { obs, ordenId, peso ->
                    viewModel.registrarServicioParaQr(
                        tecnicoId = authState.userId.takeIf { id -> id > 0 },
                        ordenId = ordenId,
                        observaciones = obs,
                        pesoInicial = peso
                    )
                }
            )
            QrSection.EXTINTORES -> {
                ExtintorHeader(onCreate = { showCreate = true }, total = extinguisherAssets.size)
                QrExtinguisherList(
                    assets = extinguisherAssets,
                    onReprint = { code -> viewModel.reprintQr(code, authState.userName, "Reimpresion desde app") },
                    onMove = { editing = it }
                )
            }
        }
    }

    if (showCreate) {
        CreateExtinguisherDialog(
            suggestedCode = "E${extinguisherAssets.size + 1}",
            onDismiss = { showCreate = false },
            onCreate = { code, owner, location, status, notes, reportResult ->
                viewModel.createExtinguisher(code, owner, location, status, authState.userName, notes) { result ->
                    if (result.isSuccess) {
                        reportResult(null)
                        showCreate = false
                    } else {
                        reportResult(result.exceptionOrNull()?.message ?: "No se pudo crear")
                    }
                }
            }
        )
    }

    editing?.let { asset ->
        MoveExtinguisherDialog(
            asset = asset,
            onDismiss = { editing = null },
            onMove = { location, status, notes, report ->
                viewModel.updateExtinguisherLocation(asset.code, location, authState.userName, notes, status) { res ->
                    if (res.isSuccess) {
                        report(null)
                        editing = null
                    } else {
                        report(res.exceptionOrNull()?.message ?: "No se pudo mover")
                    }
                }
            }
        )
    }
}

@Composable
private fun QrServicesPanel(
    qrState: QrUiState,
    extintores: List<ExtinguisherAsset>,
    onScan: (String) -> Unit,
    onRegister: (observaciones: String?, ordenId: Int?, pesoInicial: String?) -> Unit
) {
    var code by remember { mutableStateOf("") }
    var observaciones by remember { mutableStateOf("") }
    var ordenIdText by remember { mutableStateOf("") }
    var pesoText by remember { mutableStateOf("") }
    val suggestions = remember(code, extintores) {
        val term = code.trim()
        if (term.isBlank()) emptyList() else extintores.filter { it.code.contains(term, ignoreCase = true) }.take(6)
    }
    var showSuggestions by remember { mutableStateOf(false) }

    ExtintorCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Registrar servicio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = {
                            code = it
                            showSuggestions = true
                        },
                        label = { Text("Código QR / número") },
                        placeholder = { Text("Ej: EXT-001") },
                        singleLine = true
                    )
                    DropdownMenu(expanded = showSuggestions && suggestions.isNotEmpty(), onDismissRequest = { showSuggestions = false }) {
                        suggestions.forEach { ext ->
                            DropdownMenuItem(
                                text = { Text("${ext.code} · ${ext.owner}") },
                                onClick = { code = ext.code; showSuggestions = false }
                            )
                        }
                    }
                }
                ExtintorButton(
                    text = if (qrState.isLoading) "Buscando..." else "Buscar",
                    onClick = { onScan(code) },
                    enabled = !qrState.isLoading,
                    variant = ButtonVariant.Primary
                )
                ExtintorButton(
                    text = "Limpiar",
                    variant = ButtonVariant.Outline,
                    onClick = {
                        code = ""; observaciones = ""; ordenIdText = ""; pesoText = ""; showSuggestions = false
                    }
                )
            }

            qrState.scanned?.let { ext ->
                Text("Extintor ${ext.codigoQr} • ${ext.tipo}/${ext.agente}", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Próximo venc.: ${ext.fechaProximoVencimiento ?: "N/D"} (${ext.estado ?: "-"})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = ordenIdText,
                    onValueChange = { ordenIdText = it },
                    label = { Text("Orden (opcional)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = { observaciones = it },
                    label = { Text("Observaciones / servicio") }
                )
                OutlinedTextField(
                    value = pesoText,
                    onValueChange = { pesoText = it },
                    label = { Text("Peso inicial (opcional)") },
                    singleLine = true
                )
                ExtintorButton(
                    text = if (qrState.isLoading) "Registrando..." else "Registrar servicio",
                    onClick = {
                        val orden = ordenIdText.toIntOrNull()
                        onRegister(observaciones.ifBlank { null }, orden, pesoText.ifBlank { null })
                    },
                    enabled = !qrState.isLoading
                )
            }

            qrState.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
            qrState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ExtintorHeader(onCreate: () -> Unit, total: Int) {
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
                text = "$total en total",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ExtintorButton(
            text = "Agregar extintor",
            icon = null,
            variant = ButtonVariant.Primary,
            onClick = onCreate
        )
    }
}
