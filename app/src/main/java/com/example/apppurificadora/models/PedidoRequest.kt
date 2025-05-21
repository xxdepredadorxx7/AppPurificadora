// En tu paquete models, crea esta nueva clase
package com.example.apppurificadora.models

data class PedidoRequest(
    val producto_id: Int,
    val cantidad: Int,
    val total: Double
)