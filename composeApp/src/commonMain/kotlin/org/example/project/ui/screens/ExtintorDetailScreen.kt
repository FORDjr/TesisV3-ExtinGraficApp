package org.example.project.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import org.example.project.data.auth.AuthManager
import org.example.project.data.maintenance.ExtinguisherAsset
import org.example.project.ui.components.ExtintorCard
import org.example.project.ui.viewmodel.MaintenanceViewModel

@Composable
fun ExtintorDetailScreen(
    extintorId: Int?,
    fallbackCode: String? = null,
    viewModel: MaintenanceViewModel = remember { MaintenanceViewModel() },
    onBack: () -> Unit = {}
) {
    val assets by viewModel.extinguishers.collectAsState()
    val authState by AuthManager.authState.collectAsState()
    var showServiceDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(extintorId) {
        viewModel.refrescarExtintores()
    }

    val asset = assets.firstOrNull { it.id == extintorId }
    val code = asset?.code ?: fallbackCode ?: extintorId?.let { "EXT-$it" } ?: "Extintor"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Detalle de extintor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        }

        ExtintorCard(elevated = true) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(code, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                asset?.let { ext -> ExtintorInfo(ext) }
                if (asset == null) {
                    Text(
                        text = "Cargando ficha desde el servidor...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { showServiceDialog = true },
                enabled = extintorId != null,
                modifier = Modifier.weight(1f)
            ) { Text("Registrar servicio") }
            OutlinedButton(onClick = { viewModel.refrescarExtintores() }, modifier = Modifier.weight(1f)) {
                Text("Actualizar")
            }
        }

        message?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (showServiceDialog && extintorId != null) {
        ServiceDialog(
            code = code,
            onDismiss = { showServiceDialog = false },
            onSubmit = { observaciones, peso ->
                viewModel.registrarServicioRapido(
                    extintorId = extintorId,
                    tecnicoId = authState.userId.takeIf { it > 0 },
                    ordenId = null,
                    observaciones = observaciones,
                    pesoInicial = peso
                ) { result ->
                    if (result.isSuccess) {
                        message = "Servicio registrado correctamente"
                        error = null
                    } else {
                        error = result.exceptionOrNull()?.message ?: "No se pudo registrar"
                        message = null
                    }
                }
                showServiceDialog = false
            }
        )
    }
}

@Composable
private fun ExtintorInfo(ext: ExtinguisherAsset) {
    val statusLabel = ext.status.toBadge()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Propietario: ${ext.owner}", style = MaterialTheme.typography.bodyMedium)
        ext.location?.let { Text("Ubicacion: $it", style = MaterialTheme.typography.bodyMedium) }
        Text("Estado logistico: ${statusLabel.first}", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = "QR: ${ext.qrInfo.code}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ServiceDialog(
    code: String,
    onDismiss: () -> Unit,
    onSubmit: (observaciones: String?, peso: String?) -> Unit
) {
    var observaciones by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Servicio para $code") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = { observaciones = it },
                    label = { Text("Observaciones") }
                )
                OutlinedTextField(
                    value = peso,
                    onValueChange = { peso = it },
                    label = { Text("Peso inicial (opcional)") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(observaciones.ifBlank { null }, peso.ifBlank { null }) }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun org.example.project.data.maintenance.ExtinguisherStatus.toBadge(): Pair<String, Color> = when (this) {
    org.example.project.data.maintenance.ExtinguisherStatus.AVAILABLE -> "Disponible" to MaterialTheme.colorScheme.primary
    org.example.project.data.maintenance.ExtinguisherStatus.IN_WORKSHOP -> "En taller" to MaterialTheme.colorScheme.secondary
    org.example.project.data.maintenance.ExtinguisherStatus.IN_FIELD_SERVICE -> "En terreno" to MaterialTheme.colorScheme.tertiary
    org.example.project.data.maintenance.ExtinguisherStatus.ON_LOAN -> "Prestamo" to MaterialTheme.colorScheme.tertiary
    org.example.project.data.maintenance.ExtinguisherStatus.OUT_OF_SERVICE -> "Fuera de servicio" to MaterialTheme.colorScheme.error
}
