package org.example.project.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.data.auth.AuthManager
import org.example.project.ui.components.ExtintorCard
import org.example.project.ui.theme.ExtintorColors
import org.example.project.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = remember { ProfileViewModel() }
) {
    val authState by AuthManager.authState.collectAsState()
    val uiState by viewModel.state.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    val usuario = uiState.usuario
    val displayName = usuario?.let { "${it.nombre} ${it.apellido}".trim() }.takeUnless { it.isNullOrBlank() } ?: authState.userName
    val displayEmail = usuario?.email ?: authState.userEmail
    val displayRole = usuario?.rol ?: authState.userRole
    val displayId = usuario?.id ?: authState.userId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ProfileHeaderCard(
            name = displayName,
            email = displayEmail,
            role = displayRole,
            userId = displayId
        )

        uiState.success?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Text(
            text = "Acciones",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(
                text = "Editar Perfil",
                icon = Icons.Default.Edit,
                isHighlighted = true,
                onClick = { showEditDialog = true }
            )
            ActionCard(
                text = "Cerrar Sesion",
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                onClick = onLogout
            )
        }

        Text(
            text = "Informacion",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        ProfileInfoCard(
            lastAccess = "Hoy, 09:30 AM",
            memberSince = "Enero 2024",
            version = "1.0.0"
        )
    }

    if (showEditDialog) {
        ProfileEditDialog(
            initialName = usuario?.nombre ?: authState.userName.substringBefore(" "),
            initialLastName = usuario?.apellido ?: authState.userName.substringAfter(" ", ""),
            initialEmail = displayEmail,
            loading = uiState.isLoading,
            onDismiss = { showEditDialog = false },
            onSubmit = { nombre, apellido, email ->
                viewModel.actualizarPerfil(nombre, apellido, email)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun ProfileHeaderCard(
    name: String,
    email: String,
    role: String,
    userId: Int
) {
    val initials = remember(name) { name.extractInitials() }

    ExtintorCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    ExtintorColors.ExtintorRed,
                                    ExtintorColors.ExtintorRedLight
                                )
                            ),
                            shape = CircleShape
                        )
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineMedium,
                        color = ExtintorColors.PureWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = if (name.isNotBlank()) name else "Juan Diaz",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (email.isNotBlank()) email else "juan.diaz@empresa.com",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ProfileBadge(text = role.ifBlank { "Administrador" })
                val code = if (userId > 0) "USR-%03d".format(userId) else "USR-001"
                ProfileBadge(text = "ID: $code")
            }
        }
    }
}

@Composable
private fun ProfileBadge(text: String) {
    Surface(
        color = ExtintorColors.ExtintorRed.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = ExtintorColors.ExtintorRed,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCard(
    text: String,
    icon: ImageVector,
    isHighlighted: Boolean = false,
    onClick: () -> Unit
) {
    val background = if (isHighlighted) ExtintorColors.ExtintorRed.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
    val contentColor = if (isHighlighted) ExtintorColors.ExtintorRed else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = background,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = if (isHighlighted) 0.dp else 1.dp,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}

@Composable
private fun ProfileInfoCard(
    lastAccess: String,
    memberSince: String,
    version: String
) {
    ExtintorCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InfoRow(label = "Ultimo acceso", value = lastAccess)
            InfoRow(label = "Miembro desde", value = memberSince)
            InfoRow(label = "Version", value = version)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ProfileEditDialog(
    initialName: String,
    initialLastName: String,
    initialEmail: String,
    loading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var nombre by remember { mutableStateOf(initialName) }
    var apellido by remember { mutableStateOf(initialLastName) }
    var email by remember { mutableStateOf(initialEmail) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar perfil") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") }
                )
                OutlinedTextField(
                    value = apellido,
                    onValueChange = { apellido = it },
                    label = { Text("Apellido") }
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(nombre.trim(), apellido.trim(), email.trim()) }, enabled = !loading) {
                Text(if (loading) "Guardando..." else "Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !loading) { Text("Cancelar") }
        }
    )
}

private fun String.extractInitials(): String {
    if (isBlank()) return "JD"
    val parts = trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> parts[0].first().uppercaseChar().toString() + parts[1].first().uppercaseChar()
        parts.size == 1 -> parts[0].first().uppercaseChar().toString()
        else -> "JD"
    }
}
