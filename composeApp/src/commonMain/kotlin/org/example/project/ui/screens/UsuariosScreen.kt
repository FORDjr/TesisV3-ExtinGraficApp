package org.example.project.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.example.project.data.models.ActualizarUsuarioRequest
import org.example.project.data.models.CrearUsuarioRequest
import org.example.project.data.models.UsuarioResponse
import org.example.project.ui.viewmodel.UsuariosViewModel

@Composable
fun UsuariosScreen(
    viewModel: UsuariosViewModel = remember { UsuariosViewModel() },
    refreshSignal: Int = 0
) {
    val state by viewModel.state.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<UsuarioResponse?>(null) }

    LaunchedEffect(refreshSignal) {
        viewModel.cargar()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Usuarios y roles", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text("Crear, editar y desactivar cuentas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Nuevo usuario")
            }
        }

        if (state.loading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }
        }

        state.error?.let {
            Surface(
                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                tonalElevation = 0.dp
            ) {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.usuarios) { usuario ->
                UsuarioCard(
                    usuario = usuario,
                    onToggle = { viewModel.toggleActivo(usuario) },
                    onEdit = {
                        editing = usuario
                        showDialog = true
                    }
                )
            }
        }
    }

    if (showDialog) {
        UsuarioDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSubmit = { form ->
                if (editing == null) {
                    viewModel.crear(
                        CrearUsuarioRequest(
                            email = form.email,
                            password = form.password,
                            nombre = form.nombre,
                            apellido = form.apellido,
                            rol = form.rol,
                            activo = form.activo
                        )
                    )
                } else {
                    viewModel.actualizar(
                        editing!!.id,
                        ActualizarUsuarioRequest(
                            email = form.email,
                            password = form.password.ifBlank { null },
                            nombre = form.nombre,
                            apellido = form.apellido,
                            rol = form.rol,
                            activo = form.activo
                        )
                    )
                }
                showDialog = false
            }
        )
    }
}

@Composable
private fun UsuarioCard(
    usuario: UsuarioResponse,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("${usuario.nombre} ${usuario.apellido}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    Text(usuario.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Rol: ${usuario.rol}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (usuario.activo) "Activo" else "Inactivo", style = MaterialTheme.typography.bodySmall)
                    Switch(checked = usuario.activo, onCheckedChange = { onToggle() })
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
            }
        }
    }
}

private data class UsuarioFormState(
    val email: String = "",
    val password: String = "",
    val nombre: String = "",
    val apellido: String = "",
    val rol: String = "USER",
    val activo: Boolean = true
)

@Composable
private fun UsuarioDialog(
    initial: UsuarioResponse?,
    onDismiss: () -> Unit,
    onSubmit: (UsuarioFormState) -> Unit
) {
    var form by remember(initial) {
        mutableStateOf(
            if (initial == null) UsuarioFormState()
            else UsuarioFormState(
                email = initial.email,
                password = "",
                nombre = initial.nombre,
                apellido = initial.apellido,
                rol = initial.rol,
                activo = initial.activo
            )
        )
    }
    var showRoles by remember { mutableStateOf(false) }
    val roles = listOf("ADMIN", "INVENTARIO", "VENTAS", "SUPERVISOR", "USER")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Crear usuario" else "Editar usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = form.nombre,
                    onValueChange = { form = form.copy(nombre = it) },
                    label = { Text("Nombre") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.apellido,
                    onValueChange = { form = form.copy(apellido = it) },
                    label = { Text("Apellido") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.email,
                    onValueChange = { form = form.copy(email = it) },
                    label = { Text("Email") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = form.password,
                    onValueChange = { form = form.copy(password = it) },
                    label = { Text(if (initial == null) "Contraseña" else "Nueva contraseña (opcional)") },
                    singleLine = true
                )
                Column {
                    Text("Rol", style = MaterialTheme.typography.labelMedium)
                    FilledTonalButton(onClick = { showRoles = true }) {
                        Text(form.rol)
                    }
                    DropdownMenu(expanded = showRoles, onDismissRequest = { showRoles = false }) {
                        roles.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r) },
                                onClick = {
                                    form = form.copy(rol = r)
                                    showRoles = false
                                }
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Activo", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = form.activo, onCheckedChange = { form = form.copy(activo = it) })
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSubmit(form)
            }) { Text("Guardar") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
