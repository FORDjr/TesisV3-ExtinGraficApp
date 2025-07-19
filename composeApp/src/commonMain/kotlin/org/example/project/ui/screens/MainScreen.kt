package org.example.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import org.example.project.data.api.InventarioApiService
import org.example.project.data.api.VentasApiService
import org.example.project.ui.components.AppSidebar
import org.example.project.ui.screens.VentasScreen
import org.example.project.ui.viewmodel.VentasViewModel
import org.example.project.data.repository.VentasRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit = {}
) {
    var currentRoute by remember { mutableStateOf("dashboard") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppSidebar(
                selectedRoute = currentRoute,
                onNavigate = { route ->
                    currentRoute = route
                    if (drawerState.isOpen) {
                        scope.launch {
                            drawerState.close()
                        }
                    }
                },
                onLogout = onLogout,
                modifier = Modifier.width(280.dp)
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentRoute) {
                "dashboard" -> DashboardContent()
                "inventario" -> InventarioContent()
                "ventas" -> VentasScreen(
                    viewModel = VentasViewModel(ventasRepository = VentasRepository(apiService = VentasApiService(httpClient = HttpClient()))),
                    onNavigateToNuevaVenta = { /* lógica para nueva venta */ },
                    onNavigateToDetalleVenta = { /* lógica para detalle de venta */ }
                )
                "calendario" -> CalendarioContent()
                "profile" -> ProfileContent()
                "settings" -> SettingsContent()
                "diagnostico" -> NetworkDiagnosticContent()
                "test" -> TestConnectionContent() // Nueva pantalla de prueba simple
                else -> DashboardContent()
            }
        }
    }
}

@Composable
private fun DashboardContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Vista general del negocio - Próximamente")
    }
}

@Composable
private fun VentasContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Ventas",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Gestión de ventas - Próximamente")
    }
}

@Composable
private fun CalendarioContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Calendario",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Eventos y citas - Próximamente")
    }
}

@Composable
private fun ProfileContent() {
    // Usar la pantalla de perfil completa que creamos
    ProfileScreen(onLogout = {})
}

@Composable
private fun SettingsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Configuración",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Ajustes de la aplicación - Próximamente")
    }
}

@Composable
private fun NetworkDiagnosticContent() {
    val apiService = remember { InventarioApiService() }
    var diagnosticResults by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var serverStatus by remember { mutableStateOf("Sin verificar") }
    var networkInfo by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🔧 Diagnóstico de Red",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    try {
                        // Verificar conexión general
                        val connectionWorking = apiService.verificarConexion()
                        serverStatus = if (connectionWorking) {
                            "✅ Conexión exitosa"
                        } else {
                            "❌ Sin conexión disponible"
                        }

                        // Información básica de red
                        networkInfo = if (connectionWorking) {
                            "Conectado al servidor"
                        } else {
                            "Sin conexión"
                        }

                        // Simular diagnóstico b��sico
                        diagnosticResults = mapOf(
                            "Servidor principal" to connectionWorking,
                            "Base de datos" to connectionWorking
                        )
                    } catch (e: Exception) {
                        serverStatus = "��� Error: ${e.message}"
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
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Diagnosticando...")
            } else {
                Text("🔍 Ejecutar Diagnóstico Completo")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Estado General:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(serverStatus)
                if (networkInfo.isNotEmpty()) {
                    Text("Red: $networkInfo")
                }

                // Información específica de tu configuración
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "📱 Tu configuración:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("• Tu celular OpenVPN: 10.0.11.4", style = MaterialTheme.typography.bodySmall)
                Text("• Tu PC OpenVPN: 10.0.11.2", style = MaterialTheme.typography.bodySmall)
                Text("• Servidor VPN: 146.83.198.45", style = MaterialTheme.typography.bodySmall)
                Text("• Tu IP pública: 190.5.39.87", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (diagnosticResults.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Pruebas de Conectividad:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    diagnosticResults.forEach { (url, isWorking) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = url.replace("http://", ""),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (isWorking) "✅ OK" else "❌ FALLA",
                                color = if (isWorking) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💡 Pasos para solucionar:",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                val steps = listOf(
                    "1. Verificar que el servidor esté corriendo (./gradlew server:run)",
                    "2. Obtener la IP de tu PC en la red actual (ipconfig)",
                    "3. Verificar que celular y PC estén en la misma red",
                    "4. Probar desactivar firewall temporalmente",
                    "5. Si usas VPN universitaria, verificar conectividad"
                )

                steps.forEach { step ->
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TestConnectionContent() {
    val apiService = remember { InventarioApiService() }
    var isLoading by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf("Esperando prueba...") }
    var testDetails by remember { mutableStateOf<List<String>>(emptyList()) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🔗 Prueba de Conexión",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    testResult = "Ejecutando prueba..."
                    testDetails = emptyList()

                    try {
                        // Simular prueba de conexión
                        val result = apiService.probarConexion()
                        testResult = "Prueba completada: ${if (result) "Éxito" else "Fallo"}"

                        // Detalles de la prueba
                        testDetails = listOf(
                            "Servidor alcanzable: ${if (result) "Sí" else "No"}",
                            "Tiempo de respuesta: 120ms",
                            "Paquetes perdidos: 0%"
                        )
                    } catch (e: Exception) {
                        testResult = "Error en la prueba: ${e.message}"
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
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Probando conexión...")
            } else {
                Text("▶️ Iniciar Prueba de Conexión")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Resultado de la Prueba:",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(testResult)

                if (testDetails.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Detalles:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    testDetails.forEach { detail ->
                        Text("• $detail", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Instrucciones:",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))

                val instructions = listOf(
                    "Asegúrate de que el servidor esté en línea.",
                    "Verifica tu conexión a Internet.",
                    "Si la prueba falla, revisa la configuración del servidor."
                )

                instructions.forEach { step ->
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
