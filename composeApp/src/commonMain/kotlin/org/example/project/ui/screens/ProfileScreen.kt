package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.data.auth.AuthManager
import org.example.project.ui.theme.ExtintorColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {}
) {
    // Obtener datos del usuario actual usando AuthState
    val authState by AuthManager.authState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header con avatar y nombre
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar grande
                Box(
                    modifier = Modifier
                        .size(120.dp)
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
                        style = MaterialTheme.typography.headlineLarge,
                        color = ExtintorColors.PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (authState.isAuthenticated && authState.userName.isNotBlank()) {
                        authState.userName
                    } else {
                        "Usuario"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (authState.isAuthenticated && authState.userEmail.isNotBlank()) {
                        authState.userEmail
                    } else {
                        "usuario@empresa.com"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Badge de rol
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = ExtintorColors.ExtintorRed.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = if (authState.isAuthenticated && authState.userRole.isNotBlank()) {
                            authState.userRole.uppercase()
                        } else {
                            "USER"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = ExtintorColors.ExtintorRed,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Información del perfil
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Información Personal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ProfileInfoItem(
                    icon = Icons.Default.Person,
                    label = "Nombre completo",
                    value = if (authState.isAuthenticated && authState.userName.isNotBlank()) {
                        authState.userName
                    } else {
                        "No disponible"
                    }
                )

                ProfileInfoItem(
                    icon = Icons.Default.Email,
                    label = "Correo electrónico",
                    value = if (authState.isAuthenticated && authState.userEmail.isNotBlank()) {
                        authState.userEmail
                    } else {
                        "No disponible"
                    }
                )

                ProfileInfoItem(
                    icon = Icons.Default.Person,
                    label = "Rol",
                    value = if (authState.isAuthenticated && authState.userRole.isNotBlank()) {
                        authState.userRole
                    } else {
                        "user"
                    }
                )

                ProfileInfoItem(
                    icon = Icons.Default.Person,
                    label = "ID de usuario",
                    value = if (authState.isAuthenticated && authState.userId > 0) {
                        authState.userId.toString()
                    } else {
                        "No disponible"
                    }
                )
            }
        }

        // Acciones
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Acciones",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar perfil")
                }

                OutlinedButton(
                    onClick = { showChangePasswordDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cambiar contraseña")
                }

                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cerrar sesión")
                }
            }
        }
    }

    // Diálogos (por implementar en el futuro)
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Editar perfil") },
            text = { Text("Esta función estará disponible próximamente.") },
            confirmButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Cambiar contraseña") },
            text = { Text("Esta función estará disponible próximamente.") },
            confirmButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
private fun ProfileInfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
