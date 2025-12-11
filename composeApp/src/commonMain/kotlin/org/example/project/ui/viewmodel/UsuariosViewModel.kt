package org.example.project.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.data.api.UsuariosApiService
import org.example.project.data.models.ActualizarUsuarioRequest
import org.example.project.data.models.CrearUsuarioRequest
import org.example.project.data.models.UsuarioResponse

data class UsuariosUiState(
    val loading: Boolean = false,
    val saving: Boolean = false,
    val usuarios: List<UsuarioResponse> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null
)

class UsuariosViewModel(
    private val api: UsuariosApiService = UsuariosApiService()
): ViewModel() {

    private val _state = MutableStateFlow(UsuariosUiState(loading = true))
    val state: StateFlow<UsuariosUiState> = _state.asStateFlow()

    init { cargar() }

    fun cargar() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null, successMessage = null)
            try {
                val lista = api.listar()
                _state.value = _state.value.copy(loading = false, usuarios = lista, error = null)
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message ?: "Error al cargar usuarios")
            }
        }
    }

    fun crear(request: CrearUsuarioRequest) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, successMessage = null)
            try {
                api.crear(request)
                cargar()
                _state.value = _state.value.copy(saving = false, successMessage = "Usuario creado")
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = e.message ?: "Error al crear usuario")
            }
        }
    }

    fun actualizar(id: Int, request: ActualizarUsuarioRequest) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, successMessage = null)
            try {
                api.actualizar(id, request)
                cargar()
                _state.value = _state.value.copy(saving = false, successMessage = "Usuario actualizado")
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = e.message ?: "Error al actualizar usuario")
            }
        }
    }

    fun toggleActivo(usuario: UsuarioResponse) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, error = null, successMessage = null)
            try {
                api.cambiarEstado(usuario.id, !usuario.activo)
                cargar()
                _state.value = _state.value.copy(saving = false, successMessage = "Estado actualizado")
            } catch (e: Exception) {
                _state.value = _state.value.copy(saving = false, error = e.message ?: "Error al cambiar estado")
            }
        }
    }
}
