package org.example.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.model.Producto
import org.example.project.data.model.ProductoUI
import org.example.project.data.models.MetodoPago
import org.example.project.ui.components.MetodoPagoSelector
import org.example.project.ui.components.ProductoDisponibleCard
import org.example.project.ui.viewmodel.VentasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaVentaScreen(
    viewModel: VentasViewModel,
    onNavigateBack: () -> Unit,
    onVentaCreada: () -> Unit = {}
) {
    val uiState by viewModel.nuevaVentaState.collectAsState()
    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.cargarProductosParaVenta()
    }

    LaunchedEffect(uiState.ventaCreada) {
        if (uiState.ventaCreada != null) {
            showSuccess = true
            viewModel.limpiarVentaCreada()
            onVentaCreada()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Nueva Venta", fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(Modifier.padding(8.dp)) {
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = "Cancelar")
                    Spacer(Modifier.width(8.dp))
                    Text("Cancelar")
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = { viewModel.crearVenta() },
                    enabled = uiState.cliente.isNotBlank()
                            && uiState.productos.isNotEmpty()
                            && uiState.metodoPago != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Crear Venta")
                    Spacer(Modifier.width(8.dp))
                    Text("Crear Venta")
                }
            }
        }
    ) { padding ->
        if (showSuccess) {
            AlertDialog(
                onDismissRequest = { showSuccess = false },
                title = { Text("¡Venta Registrada!") },
                text = { Text("La venta se guardó correctamente.") },
                confirmButton = {
                    TextButton(onClick = { showSuccess = false }) {
                        Text("Aceptar")
                    }
                }
            )
        }

        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Cliente ---
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Cliente", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = uiState.cliente,
                            onValueChange = viewModel::actualizarCliente,
                            label = { Text("Nombre del cliente *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // --- Método de Pago ---
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Método de Pago", style = MaterialTheme.typography.titleMedium)
                        MetodoPagoSelector(uiState.metodoPago, viewModel::actualizarMetodoPago)
                    }
                }
            }

            // --- Productos Seleccionados (nombre + − / +) ---
            if (uiState.productos.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(6.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Productos Seleccionados", style = MaterialTheme.typography.titleMedium)
                            uiState.productos.forEach { prod ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = prod.nombre,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    IconButton(
                                        onClick = {
                                            if (prod.cantidad > 1)
                                                viewModel.actualizarCantidadProducto(prod.id, prod.cantidad - 1)
                                        },
                                        enabled = prod.cantidad > 1
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Disminuir")
                                    }
                                    Text(
                                        text = prod.cantidad.toString(),
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    IconButton(
                                        onClick = {
                                            if (prod.cantidad < prod.stock)
                                                viewModel.actualizarCantidadProducto(prod.id, prod.cantidad + 1)
                                        },
                                        enabled = prod.cantidad < prod.stock
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Aumentar")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- Buscador ---
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::buscarProductos,
                        label = { Text("Buscar productos") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            // --- Productos Disponibles ---
            item {
                Text("Productos Disponibles", style = MaterialTheme.typography.titleMedium)
            }
            val disponibles = uiState.productosDisponibles
                .filterNot { disp -> uiState.productos.any { it.id == disp.id } }
                .filter { disp ->
                    uiState.searchQuery.isBlank() ||
                            disp.nombre.contains(uiState.searchQuery, true) ||
                            disp.descripcion?.contains(uiState.searchQuery, true) == true
                }
            items(disponibles) { prodUI ->
                ProductoDisponibleCard(
                    producto = prodUI,
                    onAgregar = {
                        // Si ya está en el carrito, solo aumentamos cantidad
                        val existing = uiState.productos.find { it.id == prodUI.id }
                        if (existing != null) {
                            viewModel.actualizarCantidadProducto(prodUI.id, existing.cantidad + 1)
                        } else {
                            viewModel.agregarProductoAVenta(
                                Producto(
                                    id = prodUI.id,
                                    nombre = prodUI.nombre,
                                    descripcion = prodUI.descripcion,
                                    precio = prodUI.precio,
                                    cantidad = 1,
                                    categoria = prodUI.categoria,
                                    fechaCreacion = prodUI.fechaIngreso,
                                    fechaActualizacion = prodUI.fechaIngreso
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}
