package org.example.project.ui.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.data.auth.AuthManager

data class LoginUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String = "",
    val userEmail: String = "",
    val isRegistering: Boolean = false
)

class LoginViewModel {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Login con base de datos
    suspend fun login(email: String, password: String): Boolean {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = "")

        return try {
            // Validaciones básicas
            if (email.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Por favor ingresa tu email"
                )
                return false
            }

            if (password.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Por favor ingresa tu contraseña"
                )
                return false
            }

            // Autenticación real con la base de datos
            val success = AuthManager.login(email, password)

            if (success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    userEmail = email,
                    errorMessage = ""
                )
                return true
            } else {
                // Obtener el error específico del AuthManager
                val authState = AuthManager.authState.value
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = authState.error.ifEmpty { "Email o contraseña incorrectos" }
                )
                return false
            }

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = "Error de conexión: ${e.message}"
            )
            return false
        }
    }

    // Registro con base de datos
    suspend fun register(email: String, password: String, nombre: String, apellido: String): Boolean {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = "", isRegistering = true)

        return try {
            // Validaciones básicas
            if (email.isBlank() || password.isBlank() || nombre.isBlank() || apellido.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRegistering = false,
                    errorMessage = "Todos los campos son requeridos"
                )
                return false
            }

            if (password.length < 6) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRegistering = false,
                    errorMessage = "La contraseña debe tener al menos 6 caracteres"
                )
                return false
            }

            if (!isValidEmail(email)) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRegistering = false,
                    errorMessage = "Por favor ingresa un email válido"
                )
                return false
            }

            // Registro real con la base de datos
            val success = AuthManager.register(email, password, nombre, apellido)

            if (success) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRegistering = false,
                    isAuthenticated = true,
                    userEmail = email,
                    errorMessage = ""
                )
                return true
            } else {
                // Obtener el error específico del AuthManager
                val authState = AuthManager.authState.value
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRegistering = false,
                    errorMessage = authState.error.ifEmpty { "Error al registrar usuario" }
                )
                return false
            }

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isRegistering = false,
                errorMessage = "Error de conexión: ${e.message}"
            )
            return false
        }
    }

    // Limpiar errores
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = "")
        AuthManager.clearError()
    }

    // Probar conexión con el servidor
    suspend fun testConnection(): Boolean {
        return AuthManager.testConnection()
    }

    // Validación de email
    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }
}
