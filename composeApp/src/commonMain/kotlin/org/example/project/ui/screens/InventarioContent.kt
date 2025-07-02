package org.example.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.data.model.ProductoUI
import org.example.project.data.model.ProductoEstado
import org.example.project.ui.viewmodel.InventarioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioContent() {
    // Crear una instancia temporal del ViewModel hasta que se configure correctamente
    val viewModel = remember { InventarioViewModel() }

    // Observar estados del ViewModel
    val productos by viewModel.productosFiltrados.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val estadisticas by viewModel.estadisticas.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header con título y botón agregar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Inventario",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${estadisticas.totalProductos} productos registrados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.height(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Agregar",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agregar Producto")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Estadísticas rápidas (ahora usando datos reales)
        StatsCards(estadisticas = estadisticas)

        Spacer(modifier = Modifier.height(24.dp))

        // Mostrar error si existe
        error?.let { errorMessage ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { viewModel.limpiarError() }
                    ) {
                        Text("Cerrar")
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Filtros y búsqueda
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Barra de búsqueda
            OutlinedTextField(
                value = searchText,
                onValueChange = viewModel::actualizarBusqueda,
                label = { Text("Buscar productos...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                enabled = !isLoading
            )

            // Filtro por categoría
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded && !isLoading },
                modifier = Modifier.width(160.dp)
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoría") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    },
                    modifier = Modifier.menuAnchor(),
                    enabled = !isLoading
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categorias.forEach { categoria ->
                        DropdownMenuItem(
                            text = { Text(categoria) },
                            onClick = {
                                viewModel.actualizarCategoria(categoria)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón de refrescar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isLoading) "Cargando..." else "Productos:",
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(
                onClick = { viewModel.cargarProductos() },
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refrescar",
                    tint = if (isLoading) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Lista de productos con indicador de carga
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(productos) { producto ->
                    ProductCard(
                        producto = producto,
                        onEdit = { /* TODO: Implementar edición */ },
                        onDelete = {
                            viewModel.eliminarProducto(
                                id = producto.id,
                                onSuccess = {
                                    // El producto ya se elimina automáticamente de la lista
                                },
                                onError = { error ->
                                    // El error ya se maneja en el ViewModel
                                }
                            )
                        }
                    )
                }

                if (productos.isEmpty() && !isLoading) {
                    item {
                        EmptyState(searchText = searchText)
                    }
                }
            }

            // Indicador de carga
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }

    // Diálogo para agregar producto (placeholder mejorado)
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Agregar Producto") },
            text = { Text("Formulario para agregar producto conectado con la API - Próximamente implementado") },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
private fun StatsCards(estadisticas: org.example.project.ui.viewmodel.EstadisticasInventario) {
    val totalProductos = estadisticas.totalProductos
    val productosBajoStock = estadisticas.productosBajoStock
    val productosAgotados = estadisticas.productosAgotados
    val valorTotal = estadisticas.valorTotal

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "Total",
            value = totalProductos.toString(),
            icon = Icons.Default.List, // Cambiado: Folder por List
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "Bajo Stock",
            value = productosBajoStock.toString(),
            icon = Icons.Default.Warning,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "Agotados",
            value = productosAgotados.toString(),
            icon = Icons.Default.Clear, // Cambiado: Cancel por Clear
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "Valor Total",
            value = "$${String.format("%.0f", valorTotal)}",
            icon = Icons.Default.Star, // Cambiado: AttachMoney por Star
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProductCard(
    producto: ProductoUI,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header del producto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = producto.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = producto.categoria,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!producto.descripcion.isNullOrEmpty()) {
                        Text(
                            text = producto.descripcion,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Acciones
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Información del producto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Stock
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.List, // Cambiado: Folder por List
                        contentDescription = "Stock",
                        modifier = Modifier.size(16.dp),
                        tint = when {
                            producto.esBajoStock -> MaterialTheme.colorScheme.error
                            producto.estado == ProductoEstado.AGOTADO -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${producto.stock} unidades",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (producto.esBajoStock) {
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = { },
                            label = { Text("Bajo Stock", style = MaterialTheme.typography.labelSmall) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                labelColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            border = null
                        )
                    }
                }

                // Precio
                Text(
                    text = producto.precioFormateado,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyState(searchText: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Search, // Cambiado: SearchOff por Search
            contentDescription = "Sin resultados",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (searchText.isEmpty()) "No hay productos registrados" else "No se encontraron productos",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (searchText.isEmpty()) "Agrega tu primer producto" else "Intenta con otros términos de búsqueda",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
