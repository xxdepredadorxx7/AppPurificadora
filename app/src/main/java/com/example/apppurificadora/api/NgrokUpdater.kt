package com.example.apppurificadora.api

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NgrokUpdater {
    private const val TAG = "NgrokUpdater"

    suspend fun fetchAndSaveNgrokUrl(context: Context): String {
        return withContext(Dispatchers.IO) {
            val retrofit = Retrofit.Builder()
                .baseUrl(Url.casa.baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(NgrokService::class.java)

            try {
                val response = service.getNgrokUrl()
                val newUrl= response.ngrok_url.trimEnd('/') + "/api/"
                PrefsHelper.saveBaseUrl(context, newUrl)
                Log.i(TAG, "Base URL actualizada a: $newUrl")
                newUrl
            } catch (e: Exception) {
                Log.e(TAG, "Error carboxy al obtener URL de ngrok, usando URL por defecto: ${Url.casa.baseUrl}", e)
                Url.casa.baseUrl
            }
        }
    }
}