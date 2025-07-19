package org.example.project.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.viewmodel.VentasViewModel
import org.example.project.data.models.*
import org.example.project.ui.components.MetricaCard
import org.example.project.ui.components.VentaItemCard
import org.example.project.ui.components.FiltrosVentasBar
import org.example.project.ui.components.NuevaVentaFAB

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentasScreen(
    viewModel: VentasViewModel,
    onNavigateToNuevaVenta: () -> Unit,
    onNavigateToDetalleVenta: (Venta) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val ventasFiltradas = remember(uiState) { viewModel.obtenerVentasFiltradas() }

    // Cargar datos iniciales
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

                IconButton(
                    onClick = { viewModel.cargarDatos() }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Actualizar",
                        modifier = if (uiState.isLoading) {
                            Modifier.size(24.dp)
                        } else Modifier
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
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cargando ventas...")
                    }
                }

                uiState.error != null -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp) // Espacio para el FAB
                    ) {
                        item {
                            Text(
                                text = "Métricas del Día",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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

                        // Header de lista
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Historial de Ventas",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${ventasFiltradas.size} de ${uiState.ventas.size}",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Lista de ventas
                        if (ventasFiltradas.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.SearchOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "No se encontraron ventas",
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
            onClick = onNavigateToNuevaVenta,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}
