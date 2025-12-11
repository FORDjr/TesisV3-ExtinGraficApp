package org.example.project.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.ui.graphics.vector.ImageVector

data class MenuItem(
    val title: String,
    val route: String,
    val icon: ImageVector,
    val subtitle: String? = null,
    val allowedRoles: Set<String>? = null
)

object NavigationItems {
    val mainMenuItems = listOf(
        MenuItem(
            title = "Dashboard",
            route = "dashboard",
            icon = Icons.Outlined.Dashboard,
            subtitle = "Resumen general",
            allowedRoles = null // todos los roles autenticados
        ),
        MenuItem(
            title = "Ventas",
            route = "ventas",
            icon = Icons.Outlined.ShoppingCart,
            subtitle = "Gestor de ventas",
            allowedRoles = setOf("ADMIN", "VENTAS", "SUPERVISOR")
        ),
        MenuItem(
            title = "Inventario",
            route = "inventario",
            icon = Icons.Outlined.Inventory2,
            subtitle = "Stock y productos",
            allowedRoles = setOf("ADMIN", "INVENTARIO", "VENTAS", "SUPERVISOR")
        ),
        MenuItem(
            title = "Stock critico",
            route = "stockCritico",
            icon = Icons.Outlined.Warning,
            subtitle = "Resolver stock bajo",
            allowedRoles = setOf("ADMIN", "INVENTARIO", "VENTAS", "SUPERVISOR")
        ),
        MenuItem(
            title = "Kardex",
            route = "kardex",
            icon = Icons.Outlined.History,
            subtitle = "Movimientos y exportes",
            allowedRoles = setOf("ADMIN", "INVENTARIO", "VENTAS", "SUPERVISOR")
        ),
        MenuItem(
            title = "Mantencion",
            route = "maintenance",
            icon = Icons.Outlined.Verified,
            subtitle = "Taller y terreno",
            allowedRoles = setOf("ADMIN", "INVENTARIO", "SUPERVISOR")
        ),
        MenuItem(
            title = "QR",
            route = "qr",
            icon = Icons.Outlined.QrCode,
            subtitle = "Servicios y altas",
            allowedRoles = setOf("ADMIN", "INVENTARIO", "SUPERVISOR")
        ),
        MenuItem(
            title = "Calendario",
            route = "calendario",
            icon = Icons.Outlined.CalendarToday,
            subtitle = "Eventos y agenda",
            allowedRoles = null // todos los roles autenticados
        ),
        MenuItem(
            title = "Diagnostico",
            route = "diagnostico",
            icon = Icons.Outlined.Analytics,
            subtitle = "Salud del sistema",
            allowedRoles = null // util para todos los roles
        ),
        MenuItem(
            title = "Usuarios",
            route = "usuarios",
            icon = Icons.Outlined.AccountCircle,
            subtitle = "Roles y acceso",
            allowedRoles = setOf("ADMIN")
        )
    )

    val userMenuItems = listOf(
        MenuItem(
            title = "Perfil",
            route = "profile",
            icon = Icons.Outlined.AccountCircle,
            subtitle = "Datos del usuario"
        ),
        MenuItem(
            title = "Configuracion",
            route = "settings",
            icon = Icons.Outlined.Settings,
            subtitle = "Preferencias"
        )
    )

    val logout = MenuItem(
        title = "Cerrar sesion",
        route = "logout",
        icon = Icons.AutoMirrored.Outlined.Logout
    )
}
