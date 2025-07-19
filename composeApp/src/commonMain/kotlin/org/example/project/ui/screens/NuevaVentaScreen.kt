package org.example.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.viewmodel.VentasViewModel
import org.example.project.data.models.*
import org.example.project.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaVentaScreen(
    viewModel: VentasViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.nuevaVentaState.collectAsState()

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && uiState.error == null && uiState.productos.isEmpty() &&
            uiState.cliente.isEmpty() && uiState.metodoPago == null
        ) {
            viewModel.cargarProductosParaVenta()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Nueva Venta") },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                }
            }
        )

        OutlinedTextField(
            value = uiState.cliente,
            onValueChange = viewModel::actualizarCliente,
            label = { Text("Nombre del Cliente *") },
            placeholder = { Text("Ingresa el nombre del cliente") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        MetodoPagoSelector(
            metodoPagoSeleccionado = uiState.metodoPago,
            onMetodoPagoChange = viewModel::actualizarMetodoPago,
            modifier = Modifier.padding(16.dp)
        )

        if (uiState.productos.isNotEmpty()) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.productos) { producto ->
                    ProductoEnVentaCard(
                        itemVenta = producto,
                        onAgregar = {
                            viewModel.actualizarCantidadProducto(producto.id, producto.cantidad + 1)
                        }
                    )
                }
            }
        }

        Button(
            onClick = viewModel::crearVenta,
            enabled = uiState.cliente.isNotEmpty() &&
                uiState.productos.isNotEmpty() &&
                uiState.metodoPago != null &&
                !uiState.isLoading,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text("Crear Venta")
        }
    }
}
