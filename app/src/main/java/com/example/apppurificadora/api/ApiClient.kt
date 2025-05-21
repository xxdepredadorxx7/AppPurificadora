package com.example.apppurificadora.api

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.apppurificadora.MainActivity
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val TAG = "ApiClient"
    private var _retrofit: ApiService? = null

    val retrofit: ApiService
        get() = _retrofit ?: throw IllegalStateException("Retrofit no ha sido inicializado, usa getClient(context) primero")

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    suspend fun getClient(context: Context): ApiService {
        // Limpiar la URL base al iniciar para evitar URLs caducadas
        PrefsHelper.resetOnAppStart(context)

        if (_retrofit != null) return _retrofit!!

        val token = PrefsHelper.getToken(context)
        if (token.isNullOrEmpty()) {
            Log.e(TAG, "No hay token disponible, redirigiendo a login")
            redirectToLogin(context)
            throw IllegalStateException("Token no disponible")
        }

        // Obtener la URL base, intentando primero con Ngrok
        val baseUrl = try {
            NgrokUpdater.fetchAndSaveNgrokUrl(context)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo obtener URL de Ngrok, usando URL por defecto: ${Url.casa.baseUrl}")
            Url.casa.baseUrl
        }

        return buildRetrofitClient(context, token, baseUrl)
    }

    suspend fun getClientWithoutToken(context: Context): ApiService {
        // Limpiar la URL base al iniciar para evitar URLs caducadas
        PrefsHelper.resetOnAppStart(context)

        if (_retrofit != null) return _retrofit!!

        // Obtener la URL base, intentando primero con Ngrok
        val baseUrl = try {
            NgrokUpdater.fetchAndSaveNgrokUrl(context)
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo obtener URL de Ngrok, usando URL por defecto: ${Url.casa.baseUrl}")
            Url.casa.baseUrl
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
            .also { _retrofit = it }
    }

    private fun buildRetrofitClient(context: Context, token: String, baseUrl: String): ApiService {
        val authInterceptor = Interceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")

            val response = chain.proceed(requestBuilder.build())

            when (response.code) {
                401 -> {
                    Log.w(TAG, "Token inválido o expirado")
                    redirectToLogin(context)
                }
                302 -> {
                    val location = response.header("Location")
                    if (location?.contains("login") == true) {
                        Log.w(TAG, "Redirección a login detectada")
                        redirectToLogin(context)
                    }
                }
            }

            response
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
            .also { _retrofit = it }
    }

    private fun redirectToLogin(context: Context) {
        PrefsHelper.clearToken(context)
        PrefsHelper.clearBaseUrl(context)
        reset()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }

    fun reset() {
        _retrofit = null
    }
}