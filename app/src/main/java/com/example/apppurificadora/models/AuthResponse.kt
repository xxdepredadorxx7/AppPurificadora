package com.example.apppurificadora.models

data class AuthResponse(
    val access_token: String,
    val token_type: String,
    val user: User
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: String? = null,          // Nuevo campo
    val direccion: String? = null,      // Nuevo campo
    val telefono: String? = null,       // Nuevo campo
    val email_verified_at: String? = null, // Nuevo campo
)

data class Producto(
    val id: Int,
    val nombre: String,
    val descripcion: String?,
    val precio: Double,
    val cantidad: Int
)
