package org.example.project.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.example.project.data.network.AuthApiClient
import org.example.project.data.models.*

data class AuthState(
    val isAuthenticated: Boolean = false,
    val userEmail: String = "",
    val userName: String = "",
    val userRole: String = "user",
    val userId: Int = 0,
    val token: String = "",
    val isLoading: Boolean = false,
    val error: String = ""
)

object AuthManager {
    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private val apiClient = AuthApiClient()

    // Login: intenta siempre online primero; si hay fallo de red, cae a modo demo
    suspend fun login(email: String, password: String): Boolean {
        _authState.value = _authState.value.copy(isLoading = true, error = "")

        val onlineResult = loginServidor(email, password)
        if (onlineResult) return true

        // Si falló por credenciales o red, intentamos modo demo solo si coincide
        val demoResult = loginModoDemo(email, password)
        if (!demoResult && !_authState.value.isAuthenticated) {
            _authState.value = _authState.value.copy(isLoading = false)
        }
        return demoResult
    }

    // Login con servidor (modo online)
    private suspend fun loginServidor(email: String, password: String): Boolean {
        return try {
            val loginData = UsuarioLogin(email = email, password = password)
            val result = apiClient.loginUsuario(loginData)

            result.fold(
                onSuccess = { response ->
                    if (response.success && response.usuario != null) {
                        _authState.value = AuthState(
                            isAuthenticated = true,
                            userEmail = response.usuario.email,
                            userName = "${response.usuario.nombre} ${response.usuario.apellido}",
                            userRole = response.usuario.rol.uppercase(),
                            userId = response.usuario.id,
                            token = response.token ?: "",
                            isLoading = false,
                            error = ""
                        )
                        println("✅ Login exitoso desde servidor: ${response.usuario.nombre}")
                        true
                    } else {
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            error = response.message
                        )
                        false
                    }
                },
                onFailure = { exception ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Error de autenticación: ${exception.message}"
                    )
                    false
                }
            )
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = "Error inesperado: ${e.message}"
            )
            false
        }
    }

    // Modo demo para dispositivos físicos sin conexión (más estricto)
    private suspend fun loginModoDemo(email: String, password: String): Boolean {
        return try {
            // Usuarios demo predefinidos ESPECÍFICOS (no cualquier combinación)
            val usuariosDemo = listOf(
                DemoUser("admin@extingrafic.com", "123456", "Admin", "ExtinGrafic", "admin"),
                DemoUser("demo@extingrafic.com", "demo123", "Usuario", "Demo", "user"),
                DemoUser("test@extingrafic.com", "test", "Test", "User", "user")
            )

            // Buscar usuario demo EXACTO (sin fallback genérico)
            val usuarioDemo = usuariosDemo.find {
                it.email.equals(email, ignoreCase = true) && it.password == password
            }

            if (usuarioDemo != null) {
                _authState.value = AuthState(
                    isAuthenticated = true,
                    userEmail = usuarioDemo.email,
                    userName = "${usuarioDemo.nombre} ${usuarioDemo.apellido}",
                    userRole = usuarioDemo.rol.uppercase(),
                    userId = 999, // ID demo
                    token = "demo_token_${System.currentTimeMillis()}",
                    isLoading = false,
                    error = ""
                )
                println("✅ Login exitoso en modo demo: ${usuarioDemo.nombre} ${usuarioDemo.apellido}")
                true
            } else {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Credenciales incorrectas. Usuarios demo: admin@extingrafic.com/123456, demo@extingrafic.com/demo123"
                )
                false
            }
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = "Error en modo demo: ${e.message}"
            )
            false
        }
    }

    data class DemoUser(
        val email: String,
        val password: String,
        val nombre: String,
        val apellido: String,
        val rol: String
    )

    // Registro con servidor
    suspend fun register(email: String, password: String, nombre: String, apellido: String): Boolean {
        _authState.value = _authState.value.copy(isLoading = true, error = "")

        try {
            val registroData = UsuarioRegistro(
                email = email,
                password = password,
                nombre = nombre,
                apellido = apellido
            )
            val result = apiClient.registrarUsuario(registroData)

            result.fold(
                onSuccess = { response ->
                    if (response.success && response.usuario != null) {
                        // Después del registro exitoso, hacer login automático
                        _authState.value = _authState.value.copy(isLoading = false, error = "")
                        return login(email, password)
                    } else {
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            error = response.message
                        )
                        return false
                    }
                },
                onFailure = { exception ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = "Error de conexión: ${exception.message}"
                    )
                    return false
                }
            )
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(
                isLoading = false,
                error = "Error inesperado: ${e.message}"
            )
            return false
        }
    }

    // Logout
    fun logout() {
        _authState.value = AuthState()
    }
    
    // Limpiar errores
    fun clearError() {
        _authState.value = _authState.value.copy(error = "")
    }
    
    // Métodos de utilidad
    fun isLoggedIn(): Boolean = _authState.value.isAuthenticated
    fun getCurrentUser(): String = _authState.value.userEmail
    fun getUserRole(): String = _authState.value.userRole
    fun getUserId(): Int = _authState.value.userId
    fun getToken(): String = _authState.value.token

    fun applyProfileUpdate(usuario: UsuarioResponse) {
        _authState.value = _authState.value.copy(
            userEmail = usuario.email,
            userName = "${usuario.nombre} ${usuario.apellido}".trim(),
            userRole = usuario.rol.uppercase(),
            userId = usuario.id
        )
    }

    // Probar conexión con el servidor
    suspend fun testConnection(): Boolean {
        return try {
            val result = apiClient.probarConexion()
            result.isSuccess
        } catch (e: Exception) {
            false
        }
    }
    
    // Cerrar recursos
    fun close() {
        apiClient.close()
    }
}
