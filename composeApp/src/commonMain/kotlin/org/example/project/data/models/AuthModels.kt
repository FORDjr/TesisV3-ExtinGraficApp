package org.example.project.data.models

import kotlinx.serialization.Serializable

// DTOs para comunicación con el servidor
@Serializable
data class UsuarioRegistro(
    val email: String,
    val password: String,
    val nombre: String,
    val apellido: String
)

@Serializable
data class UsuarioLogin(
    val email: String,
    val password: String
)

@Serializable
data class UsuarioResponse(
    val id: Int,
    val email: String,
    val nombre: String,
    val apellido: String,
    val rol: String,
    val activo: Boolean,
    val fechaCreacion: String,
    val intentosFallidos: Int = 0,
    val bloqueadoHasta: String? = null
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val usuario: UsuarioResponse? = null,
    val token: String? = null
)

@Serializable
data class RegistroResponse(
    val success: Boolean,
    val message: String,
    val usuario: UsuarioResponse? = null
)
