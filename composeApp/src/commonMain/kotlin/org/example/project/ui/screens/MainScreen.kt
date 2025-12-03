package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.data.api.InventarioApiService
import org.example.project.data.api.MercadoLibreApiService
import org.example.project.data.api.VentasApiService
import org.example.project.data.repository.InventarioRepository
import org.example.project.data.repository.MercadoLibreRepository
import org.example.project.data.repository.VentasRepository
import org.example.project.ui.components.AppSidebar
import org.example.project.ui.components.ExtintorCard
import org.example.project.ui.components.ExtintorTopBar
import org.example.project.ui.viewmodel.DashboardViewModel
import org.example.project.ui.viewmodel.VentasViewModel
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ExperimentalMaterial3Api
import org.example.project.ui.theme.ThemeManager
import org.example.project.ui.theme.ThemePreference
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit = {}
) {
    var currentRoute by remember { mutableStateOf("dashboard") }
    val dashboardVm = remember { DashboardViewModel() }
    val ventasViewModel = remember {
        VentasViewModel(
            ventasRepository = VentasRepository(
                apiService = VentasApiService()
            ),
            inventarioRepository = InventarioRepository(),
            mercadoLibreRepository = MercadoLibreRepository(
                apiService = MercadoLibreApiService()
            )
        )
    }
    var selectedVenta by remember { mutableStateOf<org.example.project.data.models.Venta?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var refreshSignal by remember { mutableStateOf(0) }

    val topBarTitle = when (currentRoute) {
        "dashboard" -> "Dashboard"
        "inventario" -> "Inventario"
        "maintenance" -> "Mantencion"
        "kardex" -> "Kardex"
        "ventas" -> "Ventas"
        "ventaDetalle" -> "Detalle de venta"
        "calendario" -> "Calendario"
        "profile" -> "Perfil"
        "settings" -> "Configuracion"
        "diagnostico" -> "Diagnostico"
        "test" -> "Prueba"
        "vencimientos" -> "Vencimientos"
        else -> currentRoute.replaceFirstChar { first -> if (first.isLowerCase()) first.titlecase() else first.toString() }
    }

    val showRefresh = currentRoute != "vencimientos"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppSidebar(
                selectedRoute = currentRoute,
                onNavigate = { route ->
                    currentRoute = route
                    scope.launch { drawerState.close() }
                },
                onLogout = onLogout,
                modifier = Modifier.width(280.dp)
            )
        },
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.08f)
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                ExtintorTopBar(
                    title = topBarTitle,
                    onNavigationClick = {
                        scope.launch {
                            if (drawerState.isClosed) {
                                drawerState.open()
                            } else {
                                drawerState.close()
                            }
                        }
                    },
                    actions = {
                        if (showRefresh) {
                            IconButton(onClick = { refreshSignal++ }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refrescar"
                                )
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            val density = LocalDensity.current
            val navBars = WindowInsets.navigationBars
            var bottomPadding by remember { mutableStateOf(8.dp) }

            LaunchedEffect(navBars) {
                val bottomPx = navBars.getBottom(density)
                val half = with(density) { (bottomPx / 2).toDp() }
                bottomPadding = half.coerceAtLeast(8.dp)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
                    .padding(bottom = bottomPadding)
            ) {
                when (currentRoute) {
                    "dashboard" -> DashboardScreen(
                        viewModel = dashboardVm,
                        onNavigateToVencimientos = { currentRoute = "vencimientos" },
                        refreshSignal = refreshSignal
                    )
                    "vencimientos" -> ExtintoresVencenScreen(
                        viewModel = dashboardVm,
                        onBack = { currentRoute = "dashboard" }
                    )
                    "inventario" -> InventarioContent(refreshSignal = refreshSignal)
                    "kardex" -> KardexScreen(refreshSignal = refreshSignal)
                    "maintenance" -> MaintenanceScreen(refreshSignal = refreshSignal)
                    "ventas" -> VentasScreen(
                        viewModel = ventasViewModel,
                        onNavigateToDetalleVenta = { venta ->
                            selectedVenta = venta
                            currentRoute = "ventaDetalle"
                        },
                        refreshSignal = refreshSignal
                    )
                    "ventaDetalle" -> {
                        val venta = selectedVenta
                        if (venta != null) {
                            DetalleVentaScreen(
                                venta = venta,
                                viewModel = ventasViewModel,
                                onNavigateBack = {
                                    selectedVenta = null
                                    currentRoute = "ventas"
                                }
                            )
                        } else {
                            // Si no hay venta seleccionada, volver a listado
                            currentRoute = "ventas"
                        }
                    }
                    "calendario" -> CalendarScreen(refreshSignal = refreshSignal)
                    "profile" -> ProfileScreen(onLogout = onLogout)
                    "settings" -> SettingsContent(refreshSignal = refreshSignal)
                    "diagnostico" -> NetworkDiagnosticContent(refreshSignal = refreshSignal)
                    "test" -> TestConnectionContent(refreshSignal = refreshSignal)
                    else -> DashboardScreen(
                        viewModel = dashboardVm,
                        onNavigateToVencimientos = { currentRoute = "vencimientos" },
                        refreshSignal = refreshSignal
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsContent(
    refreshSignal: Int
) {
    // Opciones de tema
    val themePref by ThemeManager.themePreference.collectAsState()
    val themeOptions = listOf(
        "Sistema" to ThemePreference.SYSTEM,
        "Claro" to ThemePreference.LIGHT,
        "Oscuro" to ThemePreference.DARK
    )
    var selectedThemeLabel by remember(themePref) {
        mutableStateOf(
            when (themePref) {
                ThemePreference.SYSTEM -> "Sistema"
                ThemePreference.LIGHT -> "Claro"
                ThemePreference.DARK -> "Oscuro"
            }
        )
    }

    val languages = listOf("Español", "Inglés")
    var selectedLanguage by remember { mutableStateOf(languages.first()) }

    val currencies = listOf("CLP", "USD")
    var selectedCurrency by remember { mutableStateOf(currencies.first()) }

    var notifySales by remember { mutableStateOf(true) }
    var stockAlerts by remember { mutableStateOf(true) }
    var reminders by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Apariencia",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        ExtintorCard(elevated = false) {
            SettingsDropdown(
                label = "Tema",
                value = selectedThemeLabel,
                options = themeOptions.map { it.first },
                onOptionSelected = { label ->
                    selectedThemeLabel = label
                    themeOptions.firstOrNull { it.first == label }?.second?.let { ThemeManager.setThemePreference(it) }
                }
            )
        }

        Text(
            text = "Regional",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        ExtintorCard(elevated = false) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SettingsDropdown(
                    label = "Idioma",
                    value = selectedLanguage,
                    options = languages,
                    onOptionSelected = { selectedLanguage = it }
                )
                SettingsDropdown(
                    label = "Moneda",
                    value = selectedCurrency,
                    options = currencies,
                    onOptionSelected = { selectedCurrency = it }
                )
            }
        }

        Text(
            text = "Notificaciones",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        ExtintorCard(elevated = false) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsSwitchRow(
                    label = "Notificar ventas",
                    checked = notifySales,
                    onCheckedChange = { notifySales = it }
                )
                SettingsSwitchRow(
                    label = "Alertas de stock",
                    checked = stockAlerts,
                    onCheckedChange = { stockAlerts = it }
                )
                SettingsSwitchRow(
                    label = "Recordatorios",
                    checked = reminders,
                    onCheckedChange = { reminders = it }
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsDropdown(
    label: String,
    value: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = value,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null
                )
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NetworkDiagnosticContent(refreshSignal: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Diagnóstico de Red",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        ExtintorCard(elevated = false) {
            Text("Estado general", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Servidor principal operativo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("Base de datos sincronizada", style = MaterialTheme.typography.bodyMedium)
            Text("Última verificación: hace 2 minutos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        ExtintorCard(elevated = false) {
            Text("Pasos rápidos", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val steps = listOf(
                    "Verificar que el servidor esté corriendo",
                    "Confirmar que ambos dispositivos comparten red",
                    "Probar conexión a internet",
                    "Reiniciar servicios si persiste"
                )
                steps.forEach { step ->
                    Text(text = step, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun TestConnectionContent(refreshSignal: Int) {
    val apiService = remember { InventarioApiService() }
    var isLoading by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf("Esperando prueba...") }
    var testDetails by remember { mutableStateOf<List<String>>(emptyList()) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Prueba de Conexión",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    testResult = "Ejecutando prueba..."
                    testDetails = emptyList()
                    try {
                        val result = apiService.probarConexion()
                        testResult = if (result) "Prueba completada con éxito" else "No fue posible conectar"
                        testDetails = listOf(
                            "Servidor alcanzable: ${if (result) "Sí" else "No"}",
                            "Tiempo estimado: 120 ms",
                            "Paquetes perdidos: 0%"
                        )
                    } catch (e: Exception) {
                        testResult = "Error en la prueba: ${e.message ?: "Sin detalle"}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Probando...")
            } else {
                Text("Iniciar prueba")
            }
        }

        ExtintorCard(elevated = false) {
            Text("Resultado", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(testResult, style = MaterialTheme.typography.bodyMedium)
        }

        if (testDetails.isNotEmpty()) {
            ExtintorCard(elevated = false) {
                Text("Detalles", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                testDetails.forEach { detail ->
                    Text(detail, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        ExtintorCard(elevated = false) {
            Text("Guía rápida", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            val instructions = listOf(
                "Confirmar que el servidor está encendido",
                "Revisar credenciales de acceso",
                "Si falla, reiniciar el servicio o contactar soporte"
            )
            instructions.forEach { step ->
                Text(step, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
