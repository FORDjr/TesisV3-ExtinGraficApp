package org.example.project.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.data.model.ExtintorVencimiento
import org.example.project.ui.viewmodel.DashboardViewModel

@Composable
fun ExtintoresVencenScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit,
    onOpenExtintor: (ExtintorVencimiento) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val items = state.extintoresVencimientoLista

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "Vencimientos",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            OutlinedButton(onClick = onBack) {
                Text("Volver")
            }
        }

        if (items.isEmpty()) {
            Text(
                text = "No hay extintores por vencer en los proximos 30 dias.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(items, key = { it.id }) { ext ->
                    ExtintorVencimientoCard(
                        item = ext,
                        onSchedule = { viewModel.agendarVisita(ext.id) },
                        onOpen = { onOpenExtintor(ext) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtintorVencimientoCard(
    item: ExtintorVencimiento,
    onSchedule: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.codigo, style = MaterialTheme.typography.titleMedium)
            Text("Cliente: ${item.cliente}", style = MaterialTheme.typography.bodySmall)
            Text("Sede: ${item.sede}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Dias restantes: ${item.dias}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onSchedule, modifier = Modifier.padding(top = 8.dp)) {
                Text("Agendar visita")
            }
        }
    }
}
