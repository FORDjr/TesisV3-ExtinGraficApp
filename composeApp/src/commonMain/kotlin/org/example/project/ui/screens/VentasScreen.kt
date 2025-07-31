package org.example.project.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.models.*
import org.example.project.ui.components.FiltrosVentasBar
import org.example.project.ui.components.MetricaCard
import org.example.project.ui.components.NuevaVentaFAB
import org.example.project.ui.components.VentaItemCard
import org.example.project.ui.viewmodel.VentasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasScreen(
    viewModel: VentasViewModel,
    onNavigateToDetalleVenta: (Venta) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val ventasFiltradas = remember(uiState) { viewModel.obtenerVentasFiltradas() }
    var mostrarNuevaVenta by remember { mutableStateOf(false) }

    // Carga inicial
    LaunchedEffect(Unit) {
        viewModel.cargarDatos()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ventas",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Gestiona tus ventas y pedidos",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { viewModel.cargarDatos() }) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Actualizar"
                    )
                }
            }

            // Contenido principal
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Cargando ventas...")
                    }
                }
                uiState.error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(uiState.error!!, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        // Métricas
                        item {
                            Text("Métricas del Día", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MetricaCard(
                                    titulo = "Ventas Hoy",
                                    valor = "$${uiState.metricas.ventasHoy.toInt()}",
                                    cambio = "${if (uiState.metricas.crecimientoVentasHoy >= 0) "+" else ""}${uiState.metricas.crecimientoVentasHoy}%",
                                    esPositivo = uiState.metricas.crecimientoVentasHoy >= 0,
                                    icono = Icons.Default.AttachMoney,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricaCard(
                                    titulo = "Órdenes",
                                    valor = "${uiState.metricas.ordenesHoy}",
                                    cambio = "${if (uiState.metricas.crecimientoOrdenes >= 0) "+" else ""}${uiState.metricas.crecimientoOrdenes}",
                                    esPositivo = uiState.metricas.crecimientoOrdenes >= 0,
                                    icono = Icons.Default.ShoppingCart,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MetricaCard(
                                    titulo = "Ticket Prom.",
                                    valor = "$${uiState.metricas.ticketPromedio.toInt()}",
                                    cambio = "${if (uiState.metricas.crecimientoTicket >= 0) "+" else ""}${uiState.metricas.crecimientoTicket}%",
                                    esPositivo = uiState.metricas.crecimientoTicket >= 0,
                                    icono = Icons.AutoMirrored.Filled.TrendingUp,
                                    modifier = Modifier.weight(1f)
                                )
                                MetricaCard(
                                    titulo = "Ventas Mes",
                                    valor = "$${(uiState.metricas.ventasMes / 1000).toInt()}K",
                                    cambio = "${if (uiState.metricas.crecimientoMes >= 0) "+" else ""}${uiState.metricas.crecimientoMes}%",
                                    esPositivo = uiState.metricas.crecimientoMes >= 0,
                                    icono = Icons.Default.CalendarToday,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Filtros
                        item {
                            FiltrosVentasBar(
                                searchQuery = uiState.searchQuery,
                                onSearchChange = viewModel::buscarVentas,
                                filtroEstado = uiState.filtroEstado,
                                onFiltroEstadoChange = viewModel::filtrarPorEstado,
                                filtroFecha = uiState.filtroFecha,
                                onFiltroFechaChange = viewModel::filtrarPorFecha
                            )
                        }

                        // Historial
                        item {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Historial de Ventas", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${ventasFiltradas.size} de ${uiState.ventas.size}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Lista de ventas
                        if (ventasFiltradas.isEmpty()) {
                            item {
                                Card(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp)
                                ) {
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(48.dp))
                                        Spacer(Modifier.height(16.dp))
                                        Text("No se encontraron ventas", fontSize = 16.sp)
                                    }
                                }
                            }
                        } else {
                            items(ventasFiltradas) { venta ->
                                VentaItemCard(
                                    venta = venta,
                                    onClick = { onNavigateToDetalleVenta(venta) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB para nueva venta
        NuevaVentaFAB(
            onClick = { mostrarNuevaVenta = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )

        // Dialog mostrando la pantalla de nueva venta
        if (mostrarNuevaVenta) {
            AlertDialog(
                onDismissRequest = { mostrarNuevaVenta = false },
                confirmButton = {}, // puedes añadir botones si lo deseas
                dismissButton = {
                    TextButton(onClick = { mostrarNuevaVenta = false }) {
                        Text("Cerrar")
                    }
                },
                title = { Text("Nueva Venta") },
                text = {
                    NuevaVentaScreen(
                        viewModel = viewModel,
                        onNavigateBack = { mostrarNuevaVenta = false }
                    )
                }
            )
        }
    }
}
