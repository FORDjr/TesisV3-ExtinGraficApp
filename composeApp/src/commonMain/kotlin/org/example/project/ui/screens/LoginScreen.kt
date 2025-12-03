package org.example.project.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.ui.viewmodel.LoginViewModel
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: LoginViewModel = LoginViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    var isRegistering by remember { mutableStateOf(false) }
    
    // Estados para login
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // Estados adicionales para registro
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val focusNombre = remember { FocusRequester() }
    val focusApellido = remember { FocusRequester() }
    val focusEmail = remember { FocusRequester() }
    val focusPassword = remember { FocusRequester() }
    val focusConfirm = remember { FocusRequester() }

    fun intentarAccion() {
        scope.launch {
            val success = if (isRegistering) {
                // Validar confirmación de contraseña
                if (password != confirmPassword) {
                    return@launch
                }
                viewModel.register(email, password, nombre, apellido)
            } else {
                viewModel.login(email, password)
            }

            if (success) {
                onLoginSuccess()
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Logo o título
                Text(
                    text = "🧯 ExtinGrafic 🧯",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = if (isRegistering) "Crear cuenta" else "Bienvenido",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Campos adicionales para registro
                if (isRegistering) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nombre") },
                        placeholder = { Text("Tu nombre") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusNombre),
                        singleLine = true,
                        isError = uiState.errorMessage.isNotEmpty(),
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusApellido.requestFocus() }
                        )
                    )

                    OutlinedTextField(
                        value = apellido,
                        onValueChange = { apellido = it },
                        label = { Text("Apellido") },
                        placeholder = { Text("Tu apellido") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusApellido),
                        singleLine = true,
                        isError = uiState.errorMessage.isNotEmpty(),
                        enabled = !uiState.isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusEmail.requestFocus() }
                        )
                    )
                }

                // Campo de email
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        viewModel.clearError()
                    },
                    label = { Text("Email") },
                    placeholder = { Text("usuario@empresa.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusEmail),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusPassword.requestFocus() }
                    ),
                    singleLine = true,
                    isError = uiState.errorMessage.isNotEmpty(),
                    enabled = !uiState.isLoading
                )

                // Campo de contraseña
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        viewModel.clearError()
                    },
                    label = { Text("Contraseña") },
                    placeholder = { Text(if (isRegistering) "Mínimo 6 caracteres" else "Ingresa tu contraseña") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusPassword),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isRegistering) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { if (isRegistering) focusConfirm.requestFocus() else intentarAccion() },
                        onDone = { intentarAccion() }
                    ),
                    singleLine = true,
                    isError = uiState.errorMessage.isNotEmpty(),
                    enabled = !uiState.isLoading,
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                            )
                        }
                    }
                )

                // Campo de confirmar contraseña (solo para registro)
                if (isRegistering) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            viewModel.clearError()
                        },
                        label = { Text("Confirmar contraseña") },
                        placeholder = { Text("Confirma tu contraseña") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusConfirm),
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { intentarAccion() }
                        ),
                        singleLine = true,
                        isError = uiState.errorMessage.isNotEmpty() || (confirmPassword.isNotEmpty() && password != confirmPassword),
                        enabled = !uiState.isLoading,
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (confirmPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña"
                                )
                            }
                        }
                    )
                }

                // Mensaje de error
                if (uiState.errorMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botón principal (Login o Registro)
                Button(
                    onClick = {
                        intentarAccion()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !uiState.isLoading && 
                            email.isNotBlank() && 
                            password.isNotBlank() &&
                            (!isRegistering || (nombre.isNotBlank() && apellido.isNotBlank() && password == confirmPassword))
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = if (isRegistering) "Crear cuenta" else "Iniciar sesión",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Botón para cambiar entre login y registro
                TextButton(
                    onClick = { 
                        isRegistering = !isRegistering
                        viewModel.clearError()
                        // Limpiar campos adicionales al cambiar de modo
                        if (!isRegistering) {
                            nombre = ""
                            apellido = ""
                            confirmPassword = ""
                        }
                    },
                    enabled = !uiState.isLoading
                ) {
                    Text(
                        text = if (isRegistering) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate",
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Indicador de estado de conexión
                if (uiState.isLoading) {
                    Text(
                        text = if (isRegistering) "Creando cuenta..." else "Verificando credenciales...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
