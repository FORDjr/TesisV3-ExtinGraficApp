package org.example.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
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
import androidx.compose.ui.window.Dialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.example.project.data.models.Venta
import org.example.project.data.models.MetodoPago
import org.example.project.ui.components.EstadoBadge
import org.example.project.ui.components.ExtintorButton
import org.example.project.ui.components.ExtintorCard
import org.example.project.ui.components.ExtintorChip
import org.example.project.ui.components.ExtintorTextField
import org.example.project.ui.components.ButtonVariant
import org.example.project.ui.viewmodel.VentasViewModel
import org.example.project.utils.Formatters.formatPesos
import org.example.project.data.model.Producto
import org.example.project.data.model.ProductoUI
import org.example.project.ui.theme.ExtintorColors

@Composable
fun VentasScreen(
    viewModel: VentasViewModel,
    onNavigateToDetalleVenta: (Venta) -> Unit,
    refreshSignal: Int = 0
) {
    val uiState by viewModel.uiState.collectAsState()
    val nuevaVentaState by viewModel.nuevaVentaState.collectAsState()
    val ventasFiltradas = remember(uiState) { viewModel.obtenerVentasFiltradas() }

    var searchText by remember { mutableStateOf(uiState.searchQuery) }
    var showNuevaVenta by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.searchQuery) {
        searchText = uiState.searchQuery
    }

    LaunchedEffect(refreshSignal) {
        if (refreshSignal > 0) {
            viewModel.cargarDatos()
        }
    }

    LaunchedEffect(nuevaVentaState.ventaCreada, showNuevaVenta) {
        if (showNuevaVenta && nuevaVentaState.ventaCreada != null) {
            showNuevaVenta = false
            viewModel.limpiarVentaCreada()
            viewModel.cargarDatos()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ExtintorTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    viewModel.buscarVentas(it)
                },
                label = "Buscar cliente...",
                placeholder = "Nombre, ID o método",
                leadingIcon = Icons.Default.Search,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Ventas Recientes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when {
                uiState.isLoading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && ventasFiltradas.isEmpty() -> {
                    ExtintorCard(elevated = false) {
                        Text(
                            text = uiState.error ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                ventasFiltradas.isEmpty() -> {
                    ExtintorCard(elevated = false) {
                        Text(
                            text = "No hay ventas registradas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(ventasFiltradas) { venta ->
                            VentaResumenCard(venta = venta) {
                                onNavigateToDetalleVenta(venta)
                            }
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                showNuevaVenta = true
                viewModel.iniciarNuevaVenta()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Nueva venta")
        }

        if (showNuevaVenta) {
            NuevaVentaDialog(
                state = nuevaVentaState,
                onDismiss = { showNuevaVenta = false },
                onClienteChange = viewModel::actualizarCliente,
                onMetodoPagoChange = viewModel::actualizarMetodoPago,
                onObservacionesChange = viewModel::actualizarObservaciones,
                onBuscarProducto = viewModel::buscarProductos,
                onAgregarProducto = { ui -> viewModel.agregarProductoAVenta(ui.toProducto()) },
                onCambiarCantidad = viewModel::actualizarCantidadProducto,
                onQuitarProducto = viewModel::removerProductoDelCarrito,
                onConfirm = { viewModel.crearVenta() }
            )
        }
    }
}

@Composable
private fun VentaResumenCard(
    venta: Venta,
    onClick: () -> Unit
) {
    ExtintorCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevated = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = venta.cliente,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ID: ${venta.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatVentaFecha(venta.fecha),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    EstadoBadge(estado = venta.estado)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatPesos(venta.total),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "${venta.metodoPago.name.lowercase().replaceFirstChar { it.uppercase() }} • ${venta.productos.size} producto${if (venta.productos.size == 1) "" else "s"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatVentaFecha(raw: String): String {
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        input.timeZone = TimeZone.getTimeZone("UTC")
        val date = input.parse(raw)
        val output = SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault())
        output.format(date ?: Date())
    } catch (_: Exception) {
        raw
    }
}

@Composable
private fun NuevaVentaDialog(
    state: org.example.project.data.models.NuevaVentaUiState,
    onDismiss: () -> Unit,
    onClienteChange: (String) -> Unit,
    onMetodoPagoChange: (MetodoPago) -> Unit,
    onObservacionesChange: (String) -> Unit,
    onBuscarProducto: (String) -> Unit,
    onAgregarProducto: (ProductoUI) -> Unit,
    onCambiarCantidad: (Int, Int) -> Unit,
    onQuitarProducto: (Int) -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .padding(horizontal = 12.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            color = ExtintorColors.SoftLavender
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Nueva venta",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Registra una venta manual mientras se integra Mercado Pago",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                ExtintorTextField(
                    value = state.cliente,
                    onValueChange = onClienteChange,
                    label = "Cliente",
                    placeholder = "Ej: Camila Reyes"
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Método de pago",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetodoPago.values().forEach { metodo ->
                            ExtintorChip(
                                text = metodo.name.lowercase().replaceFirstChar { it.uppercase() },
                                selected = state.metodoPago == metodo,
                                onClick = { onMetodoPagoChange(metodo) }
                            )
                        }
                    }
                }

                ExtintorTextField(
                    value = state.observaciones,
                    onValueChange = onObservacionesChange,
                    label = "Observaciones",
                    placeholder = "Opcional",
                    singleLine = false
                )

                if (state.productos.isNotEmpty()) {
                    ExtintorCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Productos seleccionados",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            state.productos.forEach { item ->
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = item.nombre,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatPesos(item.precio.toLong()),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { onCambiarCantidad(item.id, item.cantidad - 1) },
                                                enabled = item.cantidad > 1
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Restar")
                                            }
                                            Text(
                                                text = item.cantidad.toString(),
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            )
                                            IconButton(
                                                onClick = { onCambiarCantidad(item.id, item.cantidad + 1) },
                                                enabled = item.cantidad < item.stock
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Sumar")
                                            }
                                        }
                                        TextButton(onClick = { onQuitarProducto(item.id) }) {
                                            Text("Quitar")
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "Total: ${formatPesos(state.total.toLong())}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                ExtintorCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ExtintorTextField(
                            value = state.searchQuery,
                            onValueChange = onBuscarProducto,
                            label = "Buscar productos",
                            placeholder = "Nombre o descripción",
                            leadingIcon = Icons.Default.Search
                        )
                        val disponibles = state.productosDisponibles
                            .filterNot { disp -> state.productos.any { it.id == disp.id } }
                            .filter { disp ->
                                state.searchQuery.isBlank() ||
                                    disp.nombre.contains(state.searchQuery, true) ||
                                    disp.descripcion?.contains(state.searchQuery, true) == true
                            }
                        if (disponibles.isEmpty()) {
                            Text(
                                text = "Sin productos disponibles",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                disponibles.forEach { prod ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = prod.nombre, fontWeight = FontWeight.Medium)
                                            Text(
                                                text = formatPesos(prod.precio.toLong()),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        ExtintorButton(
                                            text = "Agregar",
                                            onClick = { onAgregarProducto(prod) },
                                            variant = ButtonVariant.Outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                state.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExtintorButton(
                        text = "Cancelar",
                        onClick = onDismiss,
                        variant = ButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                    ExtintorButton(
                        text = if (state.isLoading) "Guardando..." else "Crear venta",
                        onClick = onConfirm,
                        enabled = state.isValid && !state.isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun ProductoUI.toProducto(): Producto = Producto(
    id = id,
    nombre = nombre,
    descripcion = descripcion,
    precio = precio,
    cantidad = stock,
    categoria = categoria,
    fechaCreacion = fechaIngreso,
    fechaActualizacion = fechaIngreso
)
