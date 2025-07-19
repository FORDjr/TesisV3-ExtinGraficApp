package org.example.project.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.MaterialTheme

// Elementos del menú principal sin dependencias de Material Icons
data class MenuItem(
    val title: String,
    val route: String,
    val icon: @Composable () -> Unit, // Cambio a Composable para usar emojis
    val description: String = ""
)

// Función helper para crear iconos con emojis
@Composable
fun EmojiIcon(emoji: String) {
    androidx.compose.material3.Text(
        text = emoji,
        style = MaterialTheme.typography.titleMedium
    )
}

object NavigationItems {
    val mainMenuItems = listOf(
        MenuItem(
            title = "Dashboard",
            route = "dashboard",
            icon = { EmojiIcon("🏠") },
            description = "Vista general del negocio"
        ),
        MenuItem(
            title = "Ventas",
            route = "ventas",
            icon = { EmojiIcon("🛒") },
            description = "Registro de ventas"
        ),
        MenuItem(
            title = "Inventario",
            route = "inventario",
            icon = { EmojiIcon("📦") },
            description = "Gestión de productos"
        ),
        MenuItem(
            title = "Calendario",
            route = "calendario",
            icon = { EmojiIcon("📅") },
            description = "Eventos y citas"
        ),
        MenuItem(
            title = "Diagnóstico",
            route = "diagnostico",
            icon = { EmojiIcon("🔧") },
            description = "Herramientas de diagnóstico de red"
        )
    )

    val userMenuItems = listOf(
        MenuItem(
            title = "Perfil",
            route = "profile",
            icon = { EmojiIcon("👤") },
            description = "Configuración de usuario"
        )
    )
}