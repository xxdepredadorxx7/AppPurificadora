package com.example.apppurificadora.api

import com.example.apppurificadora.models.NgrokResponse
import retrofit2.http.GET

interface NgrokService {
    @GET("data")
    suspend fun getNgrokUrl(): NgrokResponse
}
