package org.example.project.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.api.UsuariosApiService
import org.example.project.data.auth.AuthManager
import org.example.project.data.models.ActualizarUsuarioRequest
import org.example.project.data.models.UsuarioResponse

data class ProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val usuario: UsuarioResponse? = null
)

class ProfileViewModel(
    private val api: UsuariosApiService = UsuariosApiService()
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        cargarPerfil()
    }

    fun cargarPerfil() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, success = null)
            try {
                val usuario = api.obtenerActual()
                _state.value = _state.value.copy(isLoading = false, usuario = usuario)
                AuthManager.applyProfileUpdate(usuario)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "No se pudo cargar el perfil")
            }
        }
    }

    fun actualizarPerfil(nombre: String, apellido: String, email: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null, success = null)
            try {
                val updated = api.actualizarActual(
                    ActualizarUsuarioRequest(
                        email = email,
                        nombre = nombre,
                        apellido = apellido
                    )
                )
                _state.value = _state.value.copy(isLoading = false, usuario = updated, success = "Perfil actualizado")
                AuthManager.applyProfileUpdate(updated)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message ?: "No se pudo actualizar el perfil")
            }
        }
    }
}
