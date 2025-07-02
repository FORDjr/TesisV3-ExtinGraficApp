package org.example.project.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.ui.components.AppSidebar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentRoute by remember { mutableStateOf("dashboard") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope() // Agregado: scope para las corrutinas

    // Layout principal con drawer lateral
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppSidebar(
                selectedRoute = currentRoute,
                onNavigate = { route ->
                    currentRoute = route
                    // Cerrar drawer en móviles después de navegar
                    if (drawerState.isOpen) {
                        scope.launch { // Corregido: usar scope en lugar de MainScope
                            drawerState.close()
                        }
                    }
                },
                modifier = Modifier.width(280.dp)
            )
        }
    ) {
        // Contenido principal
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentRoute) {
                "dashboard" -> DashboardContent()
                "inventario" -> InventarioContent()
                "ventas" -> VentasContent()
                "calendario" -> CalendarioContent()
                "profile" -> ProfileContent()
                "settings" -> SettingsContent()
                else -> DashboardContent()
            }
        }
    }
}

// Pantallas placeholder que vamos a implementar
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Perfil",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Configuración de usuario - Próximamente")
    }
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
