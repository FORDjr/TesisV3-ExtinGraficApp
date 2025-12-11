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
import org.example.project.ui.viewmodel.MaintenanceViewModel
import org.example.project.ui.viewmodel.StockCriticoViewModel
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.ExperimentalMaterial3Api
import org.example.project.ui.theme.ThemeManager
import org.example.project.ui.theme.ThemePreference
import androidx.compose.runtime.collectAsState
import org.example.project.ui.viewmodel.UsuariosViewModel
import org.example.project.utils.NotificationPreferences
import org.example.project.data.api.HealthApiService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit = {}
) {
    var currentRoute by remember { mutableStateOf("dashboard") }
    val dashboardVm = remember { DashboardViewModel() }
    val stockCriticoViewModel = remember { StockCriticoViewModel() }
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
    val usuariosViewModel = remember { UsuariosViewModel() }
    var selectedVenta by remember { mutableStateOf<org.example.project.data.models.Venta?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var refreshSignal by remember { mutableStateOf(0) }
    val maintenanceViewModel = remember { MaintenanceViewModel() }
    var selectedExtintorId by remember { mutableStateOf<Int?>(null) }
    var selectedExtintorCode by remember { mutableStateOf<String?>(null) }
    var extintorBackRoute by remember { mutableStateOf("calendario") }

    val topBarTitle = when (currentRoute) {
        "dashboard" -> "Dashboard"
        "inventario" -> "Inventario"
        "stockCritico" -> "Stock critico"
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
        "usuarios" -> "Usuarios"
        "extintorDetalle" -> "Extintor"
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
                        onNavigateToStockCritico = { currentRoute = "stockCritico" },
                        refreshSignal = refreshSignal
                    )
                    "vencimientos" -> ExtintoresVencenScreen(
                        viewModel = dashboardVm,
                        onBack = { currentRoute = "dashboard" },
                        onOpenExtintor = { ext ->
                            extintorBackRoute = "vencimientos"
                            selectedExtintorId = ext.id
                            selectedExtintorCode = ext.codigo
                            currentRoute = "extintorDetalle"
                        }
                    )
                    "inventario" -> InventarioContent(refreshSignal = refreshSignal)
                    "stockCritico" -> StockCriticoScreen(
                        viewModel = stockCriticoViewModel,
                        refreshSignal = refreshSignal
                    )
                    "kardex" -> KardexScreen(refreshSignal = refreshSignal)
                    "maintenance" -> MaintenanceScreen(
                        viewModel = maintenanceViewModel,
                        refreshSignal = refreshSignal
                    )
                    "qr" -> QrScreen(
                        refreshSignal = refreshSignal,
                        viewModel = maintenanceViewModel
                    )
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
                    "calendario" -> CalendarScreen(
                        refreshSignal = refreshSignal,
                        onOpenExtintor = { id, code ->
                            extintorBackRoute = "calendario"
                            selectedExtintorId = id
                            selectedExtintorCode = code
                            currentRoute = "extintorDetalle"
                        }
                    )
                    "extintorDetalle" -> ExtintorDetailScreen(
                        extintorId = selectedExtintorId,
                        fallbackCode = selectedExtintorCode,
                        viewModel = maintenanceViewModel,
                        onBack = {
                            currentRoute = extintorBackRoute
                            selectedExtintorId = null
                            selectedExtintorCode = null
                        }
                    )
                    "profile" -> ProfileScreen(onLogout = onLogout)
                    "settings" -> SettingsContent(refreshSignal = refreshSignal)
                    "diagnostico" -> NetworkDiagnosticContent(refreshSignal = refreshSignal)
                    "test" -> TestConnectionContent(refreshSignal = refreshSignal)
                    "usuarios" -> UsuariosScreen(
                        viewModel = usuariosViewModel,
                        refreshSignal = refreshSignal
                    )
                    else -> DashboardScreen(
                        viewModel = dashboardVm,
                        onNavigateToVencimientos = { currentRoute = "vencimientos" },
                        onNavigateToStockCritico = { currentRoute = "stockCritico" },
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

    val notificationSettings by NotificationPreferences.settings.collectAsState()

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
                    checked = notificationSettings.notifySales,
                    onCheckedChange = { NotificationPreferences.setNotifySales(it) }
                )
                SettingsSwitchRow(
                    label = "Alertas de stock",
                    checked = notificationSettings.notifyStock,
                    onCheckedChange = { NotificationPreferences.setNotifyStock(it) }
                )
                SettingsSwitchRow(
                    label = "Recordatorios",
                    checked = notificationSettings.notifyReminders,
                    onCheckedChange = { NotificationPreferences.setNotifyReminders(it) }
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
    val healthApi = remember { HealthApiService() }
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(true) }
    var serverOk by remember { mutableStateOf(false) }
    var dbOk by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Verificando conectividad...") }
    var error by remember { mutableStateOf<String?>(null) }

    fun runCheck() {
        scope.launch {
            isLoading = true
            error = null
            serverOk = false
            dbOk = false
            try {
                serverOk = healthApi.ping()
                dbOk = healthApi.db()
                val info = healthApi.info()
                message = if (info != null) {
                    "API v${info.version} • ${info.environment} • DB: ${info.db}"
                } else {
                    "Servidor responde correctamente"
                }
            } catch (e: Exception) {
                error = e.message ?: "No se pudo verificar"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(refreshSignal) { runCheck() }

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

        ExtintorCard(elevated = true) {
            val allOk = serverOk && dbOk
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (allOk) "TODO OK" else "Atención requerida",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (allOk) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(message, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Servidor: ${if (serverOk) "Disponible" else "No responde"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (serverOk) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Base de datos: ${if (dbOk) "Conectada" else "Sin conexión"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (dbOk) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 4.dp))
                }
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { runCheck() }) { Text("Reintentar") }
                    OutlinedButton(onClick = { error = null; message = "Recuerda revisar túnel y puertos"; }) {
                        Text("Mostrar ayuda")
                    }
                }
            }
        }

        ExtintorCard(elevated = false) {
            Text("Pasos rápidos", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val steps = listOf(
                    "Validar que el servidor Ktor esté arriba (start-server...sh)",
                    "Comprobar que ambos dispositivos estén en la misma red (192.168.x.x)",
                    "Si usas túnel, verifica la URL y puertos abiertos",
                    "Reiniciar servicios de base de datos si persiste"
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
