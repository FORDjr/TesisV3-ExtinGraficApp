package org.example.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt
import org.example.project.data.model.EstadoProductoRemote
import org.example.project.data.model.ProductoEstado
import org.example.project.data.model.ProductoUI
import org.example.project.ui.components.*
import org.example.project.ui.theme.ExtintorColors
import org.example.project.ui.viewmodel.EstadisticasInventario
import org.example.project.ui.viewmodel.InventarioViewModel

@Composable
fun InventarioContent(refreshSignal: Int = 0) {
    val viewModel = remember { InventarioViewModel() }

    val productos by viewModel.productosFiltrados.collectAsState()
    val searchText by viewModel.searchText.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val estadoFiltro by viewModel.estadoFiltro.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val estadisticas by viewModel.estadisticas.collectAsState()
    val offline by viewModel.offlineMode.collectAsState()
    val hasMore by viewModel.hasMore.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showEditChoice by remember { mutableStateOf(false) }
    var showRestockDialog by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<ProductoUI?>(null) }

    val listState = rememberLazyListState()
    val estadoOptions = remember {
        listOf(
            "Activos" to EstadoProductoRemote.ACTIVO,
            "Inactivos" to EstadoProductoRemote.INACTIVO,
            "Todos" to null
        )
    }
    val categoryOptions = remember(categorias) {
        buildList {
            add("Todos")
            addAll(categorias.filter { it.isNotBlank() })
        }
    }

    LaunchedEffect(refreshSignal) { if (refreshSignal > 0) viewModel.cargarProductos() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        state = listState
    ) {
        if (offline) {
            item {
                ExtintorCard(elevated = false) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = "Modo offline",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error ?: "Sin conexión al servidor. Los datos pueden estar desactualizados.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        item { SummarySection(estadisticas) }

        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                ExtintorTextField(
                    value = searchText,
                    onValueChange = viewModel::updateSearchText,
                    label = "Buscar productos",
                    placeholder = "Nombre, categoria...",
                    leadingIcon = Icons.Default.Search,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 140.dp)
                )
                ExtintorButton(
                    text = "Agregar",
                    onClick = { showAddDialog = true },
                    icon = Icons.Default.Add,
                    variant = ButtonVariant.Primary,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (categoryOptions.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categoryOptions) { category ->
                            val isAll = category == "Todos"
                            ExtintorChip(
                                text = category,
                                selected = if (isAll) selectedCategory == "Todas" else selectedCategory == category,
                                onClick = {
                                    if (isAll) viewModel.updateSelectedCategory(null) else viewModel.updateSelectedCategory(category)
                                }
                            )
                        }
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(estadoOptions) { (label, value) ->
                        ExtintorChip(
                            text = label,
                            selected = when (value) {
                                null -> estadoFiltro == null
                                else -> estadoFiltro == value
                            },
                            onClick = { viewModel.updateEstadoFiltro(value) }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = if (isLoading && productos.isEmpty()) "Cargando inventario..." else "Productos (${productos.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            isLoading && productos.isEmpty() -> {
                item { BoxLoading() }
            }
            error != null && productos.isEmpty() -> {
                item { ErrorCard(message = error ?: "Error desconocido") }
            }
            productos.isEmpty() -> {
                item { EmptyInventoryCard() }
            }
            else -> {
                items(productos) { producto ->
                    ProductoCard(
                        producto = producto,
                        onEdit = {
                            selectedProduct = producto
                            showEditChoice = true
                        },
                        onDelete = { viewModel.eliminarProducto(producto.id) }
                    )
                }
                if (isLoading) {
                    item {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            color = ExtintorColors.ExtintorRed
                        )
                    }
                } else if (hasMore) {
                    item {
                        ExtintorButton(
                            text = "Cargar más",
                            onClick = { viewModel.cargarMas() },
                            variant = ButtonVariant.Outline,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showEditChoice && selectedProduct != null) {
        EditChoiceDialog(
            onDismiss = {
                showEditChoice = false
                selectedProduct = null
            },
            onFullEdit = {
                showEditChoice = false
                showEditDialog = true
            },
            onRestock = {
                showEditChoice = false
                showRestockDialog = true
            }
        )
    }

    if (showRestockDialog && selectedProduct != null) {
        RestockDialog(
            producto = selectedProduct!!,
            onDismiss = {
                showRestockDialog = false
                selectedProduct = null
            },
            onConfirm = { delta ->
                val nuevoStock = (selectedProduct!!.stock + delta).coerceAtLeast(0)
                viewModel.actualizarStock(selectedProduct!!.id, nuevoStock) { ok ->
                    if (ok) {
                        showRestockDialog = false
                        selectedProduct = null
                    }
                }
            }
        )
    }

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
private fun SummarySection(estadisticas: EstadisticasInventario) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Resumen de inventario",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        BoxWithConstraints {
            val spacing = 24.dp
            val cardWidth = if (maxWidth > spacing) (maxWidth - spacing) / 3 else maxWidth / 3
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Total",
                    value = estadisticas.totalProductos.toString(),
                    modifier = Modifier.width(cardWidth)
                )
                SummaryCard(
                    title = "Stock bajo",
                    value = estadisticas.productosBajoStock.toString(),
                    modifier = Modifier.width(cardWidth)
                )
                SummaryCard(
                    title = "Agotados",
                    value = estadisticas.productosAgotados.toString(),
                    modifier = Modifier.width(cardWidth)
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    ExtintorCard(
        modifier = modifier,
        elevated = true
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BoxLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = ExtintorColors.ExtintorRed)
    }
}

@Composable
private fun ErrorCard(message: String) {
    ExtintorCard(elevated = false) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column {
                Text(
                    text = "Error al cargar productos",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyInventoryCard() {
    ExtintorCard(elevated = false) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Sin productos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Agrega tu primer producto al inventario",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProductoCard(
    producto: ProductoUI,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canDelete = producto.stock > 0
    val (statusText, statusType) = when {
        producto.stock == 0 -> "Agotado" to StatusType.Error
        producto.esBajoStock -> "Critico" to StatusType.Warning
        producto.estado == ProductoEstado.INACTIVO -> "Inactivo" to StatusType.Warning
        else -> "OK" to StatusType.Success
    }

    val stockGoal = max(producto.stockMinimo * 3, producto.stockMinimo + 5)
    val ratio = if (stockGoal <= 0) 0f else producto.stock.toFloat() / stockGoal
    val progress = ratio.coerceIn(0f, 1f)
    val progressPercent = (ratio * 100).roundToInt().coerceIn(0, 100)

    ExtintorCard(
        modifier = modifier.fillMaxWidth(),
        elevated = true
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.align(Alignment.TopStart)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = producto.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        StatusBadge(text = statusText, status = statusType)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ID: ${producto.id} | ${producto.categoria}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.align(Alignment.TopEnd),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = onDelete, enabled = canDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = if (canDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            producto.descripcion?.takeIf { it.isNotBlank() }?.let { descripcion ->
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Precio",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = producto.precioFormateado,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Stock",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = producto.stock.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = ExtintorColors.ExtintorRed,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                text = "$progressPercent% del nivel optimo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
