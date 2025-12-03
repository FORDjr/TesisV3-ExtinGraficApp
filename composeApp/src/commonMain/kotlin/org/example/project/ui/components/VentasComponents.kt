package org.example.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.models.*
import org.example.project.data.model.Producto
import org.example.project.utils.Formatters.formatPesos
import java.text.SimpleDateFormat
import java.util.*
import java.util.Locale
import org.example.project.ui.theme.ExtintorDefaults
import org.example.project.ui.theme.ExtintorElevation

@Composable
fun MetricaCard(
    titulo: String,
    valor: String,
    cambio: String,
    esPositivo: Boolean,
    icono: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = titulo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    icono,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = valor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = cambio,
                fontSize = 11.sp,
                color = if (esPositivo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun VentaItemCard(
    venta: Venta,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = venta.id,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = venta.cliente,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatearFecha(venta.fecha),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    EstadoBadge(estado = venta.estado)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatPesos(venta.total),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = venta.metodoPago.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (venta.productos.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${venta.productos.size} producto${if (venta.productos.size > 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EstadoBadge(estado: EstadoVenta) {
    val (containerColor, contentColor, label) = when (estado) {
        EstadoVenta.COMPLETADA -> Triple(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.primary,
            "Completada"
        )
        EstadoVenta.PENDIENTE -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Pendiente"
        )
        EstadoVenta.CANCELADA -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Cancelada"
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = Modifier.padding(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltrosVentasBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filtroEstado: EstadoVenta?,
    onFiltroEstadoChange: (EstadoVenta?) -> Unit,
    filtroFecha: FiltroFecha,
    onFiltroFechaChange: (FiltroFecha) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search by client
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Buscar por cliente o ID...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Filtros
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status filter
            var expandedEstado by remember { mutableStateOf(false) }

            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = filtroEstado?.name?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "Todos",
                    onValueChange = { },
                    label = { Text("Estado") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { expandedEstado = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
                )

                DropdownMenu(
                    expanded = expandedEstado,
                    onDismissRequest = { expandedEstado = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todos") },
                        onClick = {
                            onFiltroEstadoChange(null)
                            expandedEstado = false
                        }
                    )
                    EstadoVenta.values().forEach { estado ->
                        DropdownMenuItem(
                            text = { Text(estado.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                onFiltroEstadoChange(estado)
                                expandedEstado = false
                            }
                        )
                    }
                }
            }

            // Date filter
            var expandedFecha by remember { mutableStateOf(false) }

            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = when (filtroFecha) {
                        FiltroFecha.TODOS -> "Todas"
                        FiltroFecha.HOY -> "Hoy"
                        FiltroFecha.SEMANA -> "Semana"
                        FiltroFecha.MES -> "Mes"
                    },
                    onValueChange = { },
                    label = { Text("Fecha") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { expandedFecha = true }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
                )

                DropdownMenu(
                    expanded = expandedFecha,
                    onDismissRequest = { expandedFecha = false }
                ) {
                    FiltroFecha.values().forEach { filtro ->
                        DropdownMenuItem(
                            text = {
                                Text(when (filtro) {
                                    FiltroFecha.TODOS -> "Todas las fechas"
                                    FiltroFecha.HOY -> "Hoy"
                                    FiltroFecha.SEMANA -> "Esta semana"
                                    FiltroFecha.MES -> "Este mes"
                                })
                            },
                            onClick = {
                                onFiltroFechaChange(filtro)
                                expandedFecha = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NuevaVentaFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Nueva Venta",
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
fun ProductoDisponibleCard(
    producto: ProductoCarrito,
    onAgregar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = producto.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                producto.descripcion?.let { desc ->
                    Text(
                        text = desc,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val precioFormateado = "$${String.format("%.2f", producto.precio)}"
                    Text(
                        text = precioFormateado,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Stock: ${producto.stock}",
                        fontSize = 12.sp,
                        color = if (producto.stock > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            IconButton(
                onClick = onAgregar,
                enabled = producto.stock > 0
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Agregar producto",
                    tint = if (producto.stock > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProductoEnVentaCard(
    itemVenta: ProductoCarrito,
    onAgregar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = itemVenta.nombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                itemVenta.descripcion?.let { desc ->
                    Text(
                        text = desc,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val precioFormateado = "$${String.format("%.2f", itemVenta.precio)}"
                    Text(
                        text = precioFormateado,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Stock: ${itemVenta.stock}",
                        fontSize = 12.sp,
                        color = if (itemVenta.stock > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            IconButton(
                onClick = onAgregar,
                enabled = itemVenta.stock > 0
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Agregar producto",
                    tint = if (itemVenta.stock > 0)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MetodoPagoSelector(
    metodoPagoSeleccionado: MetodoPago?,
    onMetodoPagoChange: (MetodoPago) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        MetodoPago.values().forEach { metodo ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMetodoPagoChange(metodo) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = metodoPagoSeleccionado == metodo,
                    onClick = { onMetodoPagoChange(metodo) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    when (metodo) {
                        MetodoPago.EFECTIVO -> Icons.Default.Money
                        MetodoPago.TARJETA -> Icons.Default.CreditCard
                        MetodoPago.TRANSFERENCIA -> Icons.Default.AccountBalance
                        MetodoPago.CREDITO -> Icons.Default.RequestQuote
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (metodo) {
                        MetodoPago.EFECTIVO -> "Efectivo"
                        MetodoPago.TARJETA -> "Tarjeta"
                        MetodoPago.TRANSFERENCIA -> "Transferencia"
                        MetodoPago.CREDITO -> "Credito"
                    },
                    fontSize = 16.sp
                )
            }
        }
    }
}

// Corrected imports for Icons and date formatting
private fun formatearFecha(fechaString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val fecha = inputFormat.parse(fechaString)
        outputFormat.format(fecha ?: Date())
    } catch (e: Exception) {
        fechaString
    }
}




