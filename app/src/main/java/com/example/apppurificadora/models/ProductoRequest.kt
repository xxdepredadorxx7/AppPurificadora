package com.example.apppurificadora.models

data class ProductoRequest(
    val id: Int,
    val nombre: String,
    val descripcion: String?,
    val precio: Double,
    val cantidad: Int
)