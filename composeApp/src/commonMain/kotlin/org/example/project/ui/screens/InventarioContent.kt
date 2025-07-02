package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.data.model.ProductoUI
import org.example.project.data.model.ProductoEstado

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioContent() {
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Todas") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Estado temporal con datos de ejemplo (luego conectaremos con la API)
    var productos by remember {
        mutableStateOf(
            listOf(
                ProductoUI(
                    id = 1,
                    nombre = "Laptop Dell XPS 13",
                    categoria = "Electrónicos",
                    stock = 15,
                    stockMinimo = 5,
                    precio = 1299.99,
                    descripcion = "Laptop profesional Dell XPS 13",
                    fechaIngreso = "2024-01-10",
                    estado = ProductoEstado.ACTIVO
                ),
                ProductoUI(
                    id = 2,
                    nombre = "Mouse Logitech MX Master",
                    categoria = "Accesorios",
                    stock = 3,
                    stockMinimo = 5,
                    precio = 99.99,
                    descripcion = "Mouse ergonómico profesional",
                    fechaIngreso = "2024-01-08",
                    estado = ProductoEstado.ACTIVO
                ),
                ProductoUI(
                    id = 3,
                    nombre = "Monitor Samsung 24\"",
                    categoria = "Electrónicos",
                    stock = 0,
                    stockMinimo = 2,
                    precio = 249.99,
                    descripcion = "Monitor Full HD 24 pulgadas",
                    fechaIngreso = "2024-01-05",
                    estado = ProductoEstado.AGOTADO
                )
            )
        )
    }

    val categorias = listOf("Todas") + productos.map { it.categoria }.distinct()
    val productosFiltrados = productos.filter { producto ->
        val matchesSearch = producto.nombre.contains(searchText, ignoreCase = true) ||
                producto.categoria.contains(searchText, ignoreCase = true)
        val matchesCategory = selectedCategory == "Todas" || producto.categoria == selectedCategory
        matchesSearch && matchesCategory
    }

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
                    text = "${productos.size} productos registrados",
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

        // Estadísticas rápidas
        StatsCards(productos = productos)

        Spacer(modifier = Modifier.height(24.dp))

        // Filtros y búsqueda
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Barra de búsqueda
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Buscar productos...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                },
                modifier = Modifier.weight(1f),
                singleLine = true
            )

            // Filtro por categoría
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
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
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categorias.forEach { categoria ->
                        DropdownMenuItem(
                            text = { Text(categoria) },
                            onClick = {
                                selectedCategory = categoria
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de productos
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(productosFiltrados) { producto ->
                ProductCard(
                    producto = producto,
                    onEdit = { /* TODO: Implementar edición */ },
                    onDelete = { /* TODO: Implementar eliminación */ }
                )
            }

            if (productosFiltrados.isEmpty()) {
                item {
                    EmptyState(searchText = searchText)
                }
            }
        }
    }

    // Diálogo para agregar producto (placeholder)
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Agregar Producto") },
            text = { Text("Formulario para agregar producto - Próximamente se conectará con la API") },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
private fun StatsCards(productos: List<ProductoUI>) {
    val totalProductos = productos.size
    val productosBajoStock = productos.count { it.esBajoStock }
    val productosAgotados = productos.count { it.estado == ProductoEstado.AGOTADO }
    val valorTotal = productos.sumOf { it.precio * it.stock }

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
