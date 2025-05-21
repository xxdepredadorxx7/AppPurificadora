package com.example.apppurificadora.models

data class PedidoResponse(
    val id: Int,
    val producto_id: Int,
    val cantidad: Int,
    val total: Double,
    val estado: String,
    val producto: ProductoRequest
)