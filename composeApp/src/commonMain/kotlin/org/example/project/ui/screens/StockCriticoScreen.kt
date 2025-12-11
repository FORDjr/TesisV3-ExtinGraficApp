package org.example.project.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import org.example.project.data.model.ProductoUI
import org.example.project.ui.components.ButtonVariant
import org.example.project.ui.components.ExtintorButton
import org.example.project.ui.components.ExtintorCard
import org.example.project.ui.components.StatusBadge
import org.example.project.ui.components.StatusType
import org.example.project.ui.viewmodel.StockCriticoViewModel

@Composable
fun StockCriticoScreen(
    viewModel: StockCriticoViewModel = remember { StockCriticoViewModel() },
    refreshSignal: Int = 0
) {
    val state by viewModel.state.collectAsState()
    var restockTarget by remember { mutableStateOf<ProductoUI?>(null) }
    var ajusteTarget by remember { mutableStateOf<ProductoUI?>(null) }

    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0) viewModel.refresh()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Stock critico",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Revisa productos bajo minimo y resuelvelos con Restock o un ajuste de Kardex.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (state.loading && state.productos.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }

        state.mensaje?.let { msg ->
            item {
                ExtintorCard(elevated = false) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        state.error?.let { err ->
            item {
                ExtintorCard(elevated = false) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "No se pudo cargar stock critico",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }

        if (!state.loading && state.productos.isEmpty() && state.error == null) {
            item {
                ExtintorCard(elevated = false) {
                    Text(
                        text = "No hay productos en stock critico.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (state.loading && state.productos.isNotEmpty()) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        items(state.productos) { producto ->
            CriticalProductCard(
                producto = producto,
                onRestock = { restockTarget = producto },
                onKardex = { ajusteTarget = producto }
            )
        }
    }

    restockTarget?.let { producto ->
        RestockCriticoDialog(
            producto = producto,
            onDismiss = { restockTarget = null },
            onConfirm = { incremento ->
                viewModel.restock(producto, incremento) { ok ->
                    if (ok) restockTarget = null
                }
            }
        )
    }

    ajusteTarget?.let { producto ->
        KardexAdjustDialog(
            producto = producto,
            onDismiss = { ajusteTarget = null },
            onConfirm = { cantidad, motivo, obs ->
                viewModel.ajustar(producto, cantidad, motivo, obs) { ok ->
                    if (ok) ajusteTarget = null
                }
            }
        )
    }
}

@Composable
private fun CriticalProductCard(
    producto: ProductoUI,
    onRestock: () -> Unit,
    onKardex: () -> Unit
) {
    ExtintorCard(elevated = true) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = producto.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "ID ${producto.id} • ${producto.categoria}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusBadge(text = "Critico", status = StatusType.Warning)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Stock actual",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${producto.stock}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Minimo definido",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${producto.stockMinimo}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LinearProgressIndicator(
                progress = {
                    val ratio = if (producto.stockMinimo <= 0) 0f else producto.stock.toFloat() / (producto.stockMinimo * 2f)
                    ratio.coerceIn(0f, 1f)
                },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.error
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExtintorButton(
                    text = "Restock",
                    onClick = onRestock,
                    icon = Icons.Filled.Add,
                    modifier = Modifier.weight(1f)
                )
                ExtintorButton(
                    text = "Kardex",
                    onClick = onKardex,
                    variant = ButtonVariant.Outline,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RestockCriticoDialog(
    producto: ProductoUI,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var incremento by remember { mutableStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Restock rapido") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Stock actual: ${producto.stock}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Cantidad a sumar",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$incremento unidades",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { incremento += 1 }) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Agregar")
                    }
                }
                Text(
                    text = "Nuevo stock estimado: ${producto.stock + incremento}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(incremento) }) {
                Text("Aplicar")
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
private fun KardexAdjustDialog(
    producto: ProductoUI,
    onDismiss: () -> Unit,
    onConfirm: (Int, String?, String?) -> Unit
) {
    var cantidadText by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var observacion by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ajuste desde Kardex") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = producto.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = cantidadText,
                    onValueChange = {
                        cantidadText = it.filter { ch -> ch.isDigit() || ch == '-' }
                        error = null
                    },
                    label = { Text("Cantidad (puede ser negativa)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it },
                    label = { Text("Motivo") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = observacion,
                    onValueChange = { observacion = it },
                    label = { Text("Observacion (opcional)") }
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
            TextButton(onClick = {
                val cantidad = cantidadText.toIntOrNull()
                if (cantidad == null || cantidad == 0) {
                    error = "Ingresa una cantidad distinta de cero"
                    return@TextButton
                }
                onConfirm(cantidad, motivo, observacion)
            }) {
                Text("Registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
