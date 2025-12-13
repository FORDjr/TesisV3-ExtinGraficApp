package org.example.project.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.launch
import org.example.project.data.models.EstadoVenta
import org.example.project.data.models.ProductoVenta
import org.example.project.data.models.Venta
import org.example.project.ui.components.EstadoBadge
import org.example.project.ui.viewmodel.VentasViewModel
import org.example.project.utils.Formatters.formatPesos
import org.example.project.preferredBaseUrl
import org.example.project.data.auth.AuthManager
import org.example.project.utils.platformContext
import org.example.project.utils.shareText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleVentaScreen(
    venta: Venta,
    viewModel: VentasViewModel,
    onNavigateBack: () -> Unit
    ) {
    val uiState by viewModel.uiState.collectAsState()
    val ventaActual = uiState.ventas.firstOrNull { it.id == venta.id } ?: venta
    val context = platformContext()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val productosDisponiblesParaDevolver = ventaActual.productos.filter { it.devuelto < it.cantidad }
    var showDevolucionDialog by remember { mutableStateOf(false) }
    var productoSeleccionadoId by remember { mutableStateOf(productosDisponiblesParaDevolver.firstOrNull()?.id) }
    var cantidadDevolucion by remember { mutableStateOf("1") }
    var motivoDevolucion by remember { mutableStateOf("") }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarMensajeEstado()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (showDevolucionDialog) {
            AlertDialog(
                onDismissRequest = { showDevolucionDialog = false },
                title = { Text("Devolución parcial") },
                text = {
                    val seleccion = productosDisponiblesParaDevolver.firstOrNull { it.id == productoSeleccionadoId }
                    var expanded by remember { mutableStateOf(false) }
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Selecciona el producto y la cantidad a devolver")
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(seleccion?.nombre ?: "Elegir producto")
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            productosDisponiblesParaDevolver.forEach { prod ->
                                DropdownMenuItem(
                                    text = { Text("${prod.nombre} (disp. ${prod.cantidad - prod.devuelto})") },
                                    onClick = {
                                        productoSeleccionadoId = prod.id
                                        val restante = (prod.cantidad - prod.devuelto).coerceAtLeast(1)
                                        cantidadDevolucion = restante.toString()
                                        expanded = false
                                    }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = cantidadDevolucion,
                            onValueChange = { cantidadDevolucion = it },
                            label = { Text("Cantidad a devolver") }
                        )
                        OutlinedTextField(
                            value = motivoDevolucion,
                            onValueChange = { motivoDevolucion = it },
                            label = { Text("Motivo (opcional)") }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val prodId = productoSeleccionadoId ?: return@TextButton
                        val prod = productosDisponiblesParaDevolver.firstOrNull { it.id == prodId } ?: return@TextButton
                        val cantidad = cantidadDevolucion.toIntOrNull() ?: 0
                        val max = (prod.cantidad - prod.devuelto).coerceAtLeast(0)
                        if (cantidad in 1..max) {
                            viewModel.registrarDevolucionParcial(
                                ventaActual.id,
                                prodId,
                                cantidad,
                                motivoDevolucion.ifBlank { null }
                            )
                            showDevolucionDialog = false
                        }
                    }) {
                        Text("Registrar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDevolucionDialog = false }) { Text("Cancelar") }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Encabezado
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Detalle de Venta",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = ventaActual.id,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                            EstadoBadge(estado = ventaActual.estado)
                        }
                    }
                }
            }

            // Información general
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Información General",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        DetalleInfoRow(
                            icono = Icons.Default.Person,
                            titulo = "Cliente",
                            valor = ventaActual.cliente
                        )
                        ventaActual.clienteFormal?.rut?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            DetalleInfoRow(
                                icono = Icons.AutoMirrored.Filled.Notes,
                                titulo = "RUT",
                                valor = it
                            )
                        }
                        ventaActual.clienteFormal?.direccion?.let {
                            Spacer(modifier = Modifier.height(12.dp))
                            DetalleInfoRow(
                                icono = Icons.AutoMirrored.Filled.Notes,
                                titulo = "Dirección",
                                valor = it
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        DetalleInfoRow(
                            icono = Icons.Default.CalendarToday,
                            titulo = "Fecha",
                            valor = formatearFechaCompleta(ventaActual.fecha)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DetalleInfoRow(
                            icono = Icons.Default.Payment,
                            titulo = "Método de Pago",
                            valor = ventaActual.metodoPago.name.lowercase()
                                .replaceFirstChar { it.uppercase() }
                        )
                        if (!ventaActual.observaciones.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            DetalleInfoRow(
                                icono = Icons.AutoMirrored.Filled.Notes,
                                titulo = "Observaciones",
                                valor = ventaActual.observaciones ?: ""
                            )
                        }
                    }
                }
            }

            // Productos
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Productos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "${ventaActual.productos.size} item${if (ventaActual.productos.size > 1) "s" else ""}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        ventaActual.productos.forEachIndexed { index, producto ->
                            ProductoDetalleCard(producto)
                            if (index < ventaActual.productos.size - 1) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }

            // Resumen
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Resumen",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        val subtotalCalculado = if (ventaActual.subtotal > 0) ventaActual.subtotal else ventaActual.productos.sumOf { it.subtotal }
                        ResumenRow(
                            titulo = "Subtotal:",
                            valor = formatPesos(subtotalCalculado)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        ResumenRow(titulo = "Impuestos:", valor = formatPesos(ventaActual.impuestos))
                        Spacer(modifier = Modifier.height(8.dp))
                        ResumenRow(titulo = "Descuento:", valor = formatPesos(ventaActual.descuento))
                        Spacer(modifier = Modifier.height(8.dp))
                        ResumenRow(titulo = "Devuelto:", valor = formatPesos(ventaActual.totalDevuelto))

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total:",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatPesos(ventaActual.total),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        ResumenRow(titulo = "Saldo:", valor = formatPesos(ventaActual.saldo))
                    }
                }
            }

            // Acciones
            item {
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Acciones",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val token = AuthManager.getToken()
                                    val tokenQuery = token.takeIf { it.isNotBlank() }?.let { "?token=$it" } ?: ""
                                    val uri = "${preferredBaseUrl()}/api/ventas/${ventaActual.id}/comprobante/pdf$tokenQuery"
                                    uriHandler.openUri(uri)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Print,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Imprimir")
                            }
                            OutlinedButton(
                                onClick = {
                                    val token = AuthManager.getToken()
                                    val tokenQuery = token.takeIf { it.isNotBlank() }?.let { "?token=$it" } ?: ""
                                    val uri = "${preferredBaseUrl()}/api/ventas/${ventaActual.id}/comprobante/pdf$tokenQuery"
                                    shareText(context, uri, "Comprobante ${ventaActual.id}")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Compartir")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (ventaActual.estado) {
                            EstadoVenta.PENDIENTE -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.actualizarEstadoVenta(ventaActual.id, EstadoVenta.CANCELADA) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.Red
                                        )
                                    ) {
                                        Text("Cancelar")
                                    }
                                    Button(
                                        onClick = { viewModel.actualizarEstadoVenta(ventaActual.id, EstadoVenta.COMPLETADA) },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Completar") }
                                }
                            }
                            EstadoVenta.COMPLETADA -> {
                                Button(
                                    onClick = {
                                        productoSeleccionadoId = productosDisponiblesParaDevolver.firstOrNull()?.id
                                        val cantidadDefault = productosDisponiblesParaDevolver.firstOrNull()?.let {
                                            (it.cantidad - it.devuelto).coerceAtLeast(1)
                                        } ?: 1
                                        cantidadDevolucion = cantidadDefault.toString()
                                        showDevolucionDialog = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = productosDisponiblesParaDevolver.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary
                                    )
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generar Devolución")
                                }
                            }
                            EstadoVenta.CANCELADA -> { /* no acciones */ }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun DetalleInfoRow(
    icono: ImageVector,
    titulo: String,
    valor: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icono,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = titulo,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ProductoDetalleCard(producto: ProductoVenta) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = producto.nombre,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${formatPesos(producto.precio)} × ${producto.cantidad}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (producto.descuento > 0) {
                Text(
                    text = "Descuento: ${formatPesos(producto.descuento)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (producto.iva > 0) {
                Text(
                    text = "IVA: ${producto.iva}%",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (producto.devuelto > 0) {
                Text(
                    text = "Devuelto: ${producto.devuelto}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = formatPesos(producto.subtotal),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun ResumenRow(titulo: String, valor: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = titulo,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = valor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

fun formatearFechaCompleta(fechaString: String): String {
    runCatching {
        val instant = Instant.parse(fechaString)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "%04d-%02d-%02d   %02d:%02d".format(
            local.date.year,
            local.date.monthNumber,
            local.date.dayOfMonth,
            local.hour,
            local.minute
        )
    }
    val cleaned = fechaString.replace("Z", "").trim()
    val parts = cleaned.split("T")
    if (parts.size == 2) {
        val date = parts[0]
        val time = parts[1].take(5)
        return "$date   $time"
    }
    return fechaString
}
