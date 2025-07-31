package org.example.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.data.model.ProductoUI
import org.example.project.data.model.ProductoEstado
import org.example.project.ui.viewmodel.InventarioViewModel
import org.example.project.ui.components.*
import org.example.project.ui.theme.ExtintorColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioContent() {
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
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<ProductoUI?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header elegante con gradiente extintor
        ExtintorGradientHeader(
            title = "Gestión de Inventario",
            subtitle = "Control total de extintores y equipos",
            icon = Icons.Default.Home
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Estadísticas en cards elegantes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExtintorCard(
                    modifier = Modifier.weight(1f),
                    elevated = true
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExtintorColors.Gray600
                    )
                    Text(
                        text = "${estadisticas.totalProductos}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ExtintorColors.CharcoalBlack
                    )
                }

                ExtintorCard(
                    modifier = Modifier.weight(1f),
                    elevated = true
                ) {
                    Text(
                        text = "Stock Bajo",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExtintorColors.Gray600
                    )
                    Text(
                        text = "${estadisticas.productosBajoStock}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ExtintorColors.Warning
                    )
                }

                ExtintorCard(
                    modifier = Modifier.weight(1f),
                    elevated = true
                ) {
                    Text(
                        text = "Agotados",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExtintorColors.Gray600
                    )
                    Text(
                        text = "${estadisticas.productosAgotados}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ExtintorColors.ExtintorRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Controles de búsqueda y filtros
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Campo de búsqueda elegante FUNCIONANDO
                ExtintorTextField(
                    value = searchText,
                    onValueChange = { viewModel.updateSearchText(it) },
                    label = "Buscar productos",
                    placeholder = "Nombre, categoría...",
                    leadingIcon = Icons.Default.Search,
                    modifier = Modifier.weight(1f)
                )

                // Botón agregar con estilo extintor FUNCIONANDO
                ExtintorButton(
                    text = "Agregar",
                    onClick = { showAddDialog = true },
                    icon = Icons.Default.Add,
                    variant = ButtonVariant.Primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mostrar productos aunque la lista de categorías esté vacía
            // Filtros de categoría con chips elegantes FUNCIONANDO
            if (categorias.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(categorias) { categoria ->
                        ExtintorChip(
                            text = categoria,
                            selected = selectedCategory == categoria,
                            onClick = { viewModel.updateSelectedCategory(categoria) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Mostrar lista de productos siempre
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = ExtintorColors.ExtintorRed
                        )
                    }
                }

                error != null -> {
                    ExtintorCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = ExtintorColors.ExtintorRed,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Error al cargar productos",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ExtintorColors.ExtintorRed
                                )
                            }
                        }
                    }
                }

                productos.isEmpty() -> {
                    ExtintorCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = ExtintorColors.Gray400,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No hay productos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = ExtintorColors.Gray600
                            )
                            Text(
                                text = "Agrega tu primer producto al inventario",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ExtintorColors.Gray500
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(productos) { producto ->
                            ProductoCard(
                                producto = producto,
                                onEdit = {
                                    selectedProduct = producto
                                    showEditDialog = true
                                },
                                onDelete = {
                                    viewModel.eliminarProducto(producto.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog para agregar producto FUNCIONANDO
    if (showAddDialog) {
        ProductDialog(
            title = "Agregar Producto",
            onDismiss = { showAddDialog = false },
            onConfirm = { producto ->
                viewModel.agregarProducto(producto)
                showAddDialog = false
            },
            categorias = categorias
        )
    }

    // Dialog para editar producto FUNCIONANDO
    if (showEditDialog && selectedProduct != null) {
        ProductDialog(
            title = "Editar Producto",
            initialProduct = selectedProduct,
            onDismiss = {
                showEditDialog = false
                selectedProduct = null
            },
            onConfirm = { producto ->
                viewModel.actualizarProducto(selectedProduct!!.id, producto)
                showEditDialog = false
                selectedProduct = null
            },
            categorias = categorias
        )
    }
}

@Composable
private fun ProductoCard(
    producto: ProductoUI,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val puedeEliminar = producto.stock > 0 // Solo permitir eliminar si el producto tiene stock
    ExtintorCard(
        modifier = modifier,
        elevated = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = producto.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = ExtintorColors.CharcoalBlack
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(
                        text = when {
                            producto.stock == 0 -> "Sin stock"
                            producto.estado == ProductoEstado.ACTIVO -> "Activo"
                            producto.estado == ProductoEstado.INACTIVO -> "Inactivo"
                            producto.estado == ProductoEstado.AGOTADO -> "Agotado"
                            else -> ""
                        },
                        status = when {
                            producto.stock == 0 -> StatusType.Error
                            producto.estado == ProductoEstado.ACTIVO -> StatusType.Success
                            producto.estado == ProductoEstado.INACTIVO -> StatusType.Neutral
                            producto.estado == ProductoEstado.AGOTADO -> StatusType.Error
                            else -> StatusType.Neutral
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "ID: ${producto.id}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ExtintorColors.Gray600
                )

                Text(
                    text = "Stock: ${producto.stock}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ExtintorColors.Gray600
                )

                Text(
                    text = "Precio: ${producto.precioFormateado}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ExtintorColors.Gray600
                )

                if (producto.descripcion != null) {
                    Text(
                        text = "Descripción: ${producto.descripcion}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ExtintorColors.Gray500
                    )
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = ExtintorColors.Gray600
                    )
                }
                IconButton(
                    onClick = onDelete,
                    enabled = puedeEliminar // Deshabilitar si no se puede eliminar
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = if (puedeEliminar) ExtintorColors.ExtintorRed else ExtintorColors.Gray400
                    )
                }
            }
        }
    }
}
