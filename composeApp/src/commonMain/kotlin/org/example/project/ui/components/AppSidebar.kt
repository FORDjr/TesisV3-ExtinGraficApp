package org.example.project.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.ui.navigation.MenuItem
import org.example.project.ui.navigation.NavigationItems
import org.example.project.ui.theme.ExtintorColors
import org.example.project.data.auth.AuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSidebar(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Obtener datos del usuario actual
    val currentUser = AuthManager.getCurrentUser()

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column {
            // Header del Sidebar
            SidebarHeader()

            HorizontalDivider() // Corregido: Divider() está deprecado

            // Contenido principal del menú
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Sección principal
                item {
                    SidebarGroupLabel("Navegación Principal")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(NavigationItems.mainMenuItems) { item ->
                    SidebarMenuItem(
                        item = item,
                        isSelected = selectedRoute == item.route,
                        onClick = { onNavigate(item.route) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SidebarGroupLabel("Cuenta")
                    Spacer(modifier = Modifier.height(8.dp))
                }

                items(NavigationItems.userMenuItems) { item ->
                    SidebarMenuItem(
                        item = item,
                        isSelected = selectedRoute == item.route,
                        onClick = { onNavigate(item.route) }
                    )
                }
            }

            HorizontalDivider() // Corregido: Divider() está deprecado

            // Footer con usuario
            SidebarFooter(onLogout = onLogout)
        }
    }
}

@Composable
private fun SidebarHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo con gradiente de extintor
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            ExtintorColors.ExtintorRed,
                            ExtintorColors.ExtintorRedLight
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🧯", // Emoji de fuego como alternativa
                style = MaterialTheme.typography.headlineLarge,
                color = ExtintorColors.PureWhite
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "ExtinGrafic",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ExtintorColors.PureWhite
        )

        Text(
            text = "Sistema de Gestión",
            style = MaterialTheme.typography.bodyMedium,
            color = ExtintorColors.Gray400
        )
    }
}

@Composable
private fun SidebarMenuItem(
    item: MenuItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cambio: usar el icono composable en lugar de Icon
            item.icon()

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )

                if (item.description.isNotEmpty()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarGroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}

@Composable
private fun SidebarFooter(onLogout: () -> Unit) {
    var showUserMenu by remember { mutableStateOf(false) }

    // Obtener datos del usuario actual usando los métodos correctos del AuthManager
    val authState by AuthManager.authState.collectAsState()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showUserMenu = !showUserMenu },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar del usuario con iniciales
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(
                                ExtintorColors.ExtintorRed,
                                ExtintorColors.ExtintorRedLight
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (authState.isAuthenticated && authState.userName.isNotBlank()) {
                        // Extraer iniciales del nombre completo
                        val names = authState.userName.split(" ")
                        if (names.size >= 2) {
                            "${names[0].first().uppercase()}${names[1].first().uppercase()}"
                        } else {
                            authState.userName.first().uppercase()
                        }
                    } else {
                        "👤"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = ExtintorColors.PureWhite,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (authState.isAuthenticated && authState.userName.isNotBlank()) {
                        authState.userName
                    } else {
                        "Usuario"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (authState.isAuthenticated && authState.userEmail.isNotBlank()) {
                        authState.userEmail
                    } else {
                        "usuario@empresa.com"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = if (showUserMenu) "▲" else "▼", // Flechas simples
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Menú desplegable del usuario (simplificado por ahora)
    if (showUserMenu) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLogout() }
                        .padding(8.dp)
                ) {
                    Text(
                        text = "🚪", // Emoji de salida
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cerrar sesión",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
