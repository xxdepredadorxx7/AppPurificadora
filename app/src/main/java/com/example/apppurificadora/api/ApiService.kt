package com.example.apppurificadora.api

import com.example.apppurificadora.models.AuthResponse
import com.example.apppurificadora.models.DataResponse
import com.example.apppurificadora.models.LoginRequest
import com.example.apppurificadora.models.PedidoRequest
import com.example.apppurificadora.models.PedidoResponse
import com.example.apppurificadora.models.ProductoRequest
import com.example.apppurificadora.models.RegisterRequest
import com.example.apppurificadora.models.UpdateProfileRequest
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("login")
    suspend fun login(@Body credentials: LoginRequest): Response<AuthResponse>

    @POST("register")
    suspend fun register(@Body user: RegisterRequest): Response<AuthResponse>

    @GET("data")
    suspend fun getData(): Response<DataResponse>

    @PUT("users/{id}")
    suspend fun updateProfile(
        @Path("id") id: Int,
        @Body request: UpdateProfileRequest
    ): Response<AuthResponse>

    // Productos
    @GET("productos")
    suspend fun getProductos(): Response<List<ProductoRequest>>

    @GET("productos/{id}")
    suspend fun getProducto(@Path("id") id: Int): Response<ProductoRequest>

    @DELETE("productos/{id}")
    suspend fun eliminarProducto(@Path("id") id: Int): Response<Unit>

    // Pedidos - CREAR/ACTUALIZAR usa PedidoRequest, RECIBIR usa PedidoResponse
    @GET("pedidos")
    suspend fun getPedidos(): Response<List<PedidoResponse>>

    @GET("pedidos/{id}")
    suspend fun getPedido(@Path("id") id: Int): Response<PedidoResponse>

    @POST("pedidos")
    suspend fun crearPedido(@Body pedido: PedidoRequest): Response<PedidoResponse>

    @PUT("pedidos/{id}")
    suspend fun actualizarPedido(@Path("id") id: Int, @Body pedido: PedidoRequest): Response<PedidoResponse>

    @DELETE("pedidos/{id}")
    suspend fun eliminarPedido(@Path("id") id: Int): Response<Unit>
}
