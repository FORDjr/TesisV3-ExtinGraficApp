package org.example.project.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

// Elementos del menú principal (equivalente a menuItems en v0)
data class MenuItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val description: String = ""
)

object NavigationItems {
    val mainMenuItems = listOf(
        MenuItem(
            title = "Dashboard",
            route = "dashboard",
            icon = Icons.Default.Home, // Cambiado a icono básico que siempre existe
            description = "Vista general del negocio"
        ),
        MenuItem(
            title = "Inventario",
            route = "inventario",
            icon = Icons.Default.List, // Cambiado: Folder por List
            description = "Gestión de productos"
        ),
        MenuItem(
            title = "Ventas",
            route = "ventas",
            icon = Icons.Default.ShoppingCart,
            description = "Registro de ventas"
        ),
        MenuItem(
            title = "Calendario",
            route = "calendario",
            icon = Icons.Default.DateRange, // Cambiado a icono básico que siempre existe
            description = "Eventos y citas"
        )
    )

    val userMenuItems = listOf(
        MenuItem(
            title = "Perfil",
            route = "profile",
            icon = Icons.Default.Person,
            description = "Configuración de usuario"
        ),
        MenuItem(
            title = "Configuración",
            route = "settings",
            icon = Icons.Default.Settings,
            description = "Ajustes de la aplicación"
        )
    )
}
