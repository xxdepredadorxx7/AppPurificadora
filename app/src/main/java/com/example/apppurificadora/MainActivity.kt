package com.example.apppurificadora

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.apppurificadora.api.ApiClient
import com.example.apppurificadora.api.PrefsHelper
import com.example.apppurificadora.databinding.ActivityMainBinding
import com.example.apppurificadora.models.LoginRequest
import com.example.apppurificadora.models.RegisterRequest
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // Agrega una constante para el tag de logging
    companion object {
        private const val TAG = "MainActivity"
        private const val LOG_PREFIX = "👀 [MainActivity]"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "$LOG_PREFIX onCreate() - Inicializando actividad")

        val baseUrl = PrefsHelper.getBaseUrl(this)
        Log.d("BaseUrlTracker", "Base URL actual: $baseUrl")

        sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        setupListeners()
        setupPasswordValidation()
    }

    private fun setupPasswordValidation() {
        Log.d(TAG, "$LOG_PREFIX Configurando validación de contraseña")
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val password = s?.toString() ?: ""
                Log.v(TAG, "$LOG_PREFIX Texto cambiado en campo contraseña: ${if (password.isEmpty()) "vacío" else "***"}")
                if (password.isNotEmpty() && isRegisterMode) {
                    Log.d(TAG, "$LOG_PREFIX Validando requisitos de contraseña en modo registro")
                    showPasswordRequirements(password)
                } else {
                    binding.tilPassword.error = null
                    binding.tilPassword.helperText = null
                }
            }
        })
    }

    private fun showPasswordRequirements(password: String) {
        Log.d(TAG, "$LOG_PREFIX Mostrando requisitos de contraseña")
        val requirements = checkPasswordRequirements(password)
        val messages = mutableListOf<String>()

        if (!requirements["length"]!!) messages.add("✗ Mínimo 8 caracteres") else messages.add("✓ 8+ caracteres")
        if (!requirements["uppercase"]!!) messages.add("✗ Mayúscula") else messages.add("✓ Mayúscula")
        if (!requirements["lowercase"]!!) messages.add("✗ Minúscula") else messages.add("✓ Minúscula")
        if (!requirements["digit"]!!) messages.add("✗ Número") else messages.add("✓ Número")
        if (!requirements["special"]!!) messages.add("✗ Símbolo") else messages.add("✓ Símbolo")

        if (requirements.all { it.value }) {
            Log.d(TAG, "$LOG_PREFIX Contraseña cumple todos los requisitos")
            binding.tilPassword.error = null
            binding.tilPassword.helperText = "✓ Contraseña válida"
            binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(Color.GREEN))
        } else {
            Log.d(TAG, "$LOG_PREFIX Contraseña no cumple requisitos: ${messages.joinToString(", ")}")
            binding.tilPassword.helperText = "Requisitos:"
            binding.tilPassword.error = messages.joinToString("\n")
        }
    }

    private fun checkPasswordRequirements(password: String): Map<String, Boolean> {
        return mapOf(
            "length" to (password.length >= 8),
            "uppercase" to password.any { it.isUpperCase() },
            "lowercase" to password.any { it.isLowerCase() },
            "digit" to password.any { it.isDigit() },
            "special" to password.any { !it.isLetterOrDigit() }
        )
    }

    private fun setupListeners() {
        Log.d(TAG, "$LOG_PREFIX Configurando listeners")
        binding.btnLogin.setOnClickListener {
            Log.d(TAG, "$LOG_PREFIX Botón de login/registro clickeado")
            if (validateInputs()) {
                val email = binding.etEmail.text.toString()
                val password = binding.etPassword.text.toString()

                if (binding.tilName.visibility == View.VISIBLE) {
                    val name = binding.etName.text.toString()
                    Log.d(TAG, "$LOG_PREFIX Iniciando proceso de registro para: $email")
                    registerUser(name, email, password)
                } else {
                    Log.d(TAG, "$LOG_PREFIX Iniciando proceso de login para: $email")
                    loginUser(email, password)
                }
            }else {
                Log.d(TAG, "$LOG_PREFIX Validación fallida, no se procede con login/registro")
            }
        }

        binding.tvRegister.setOnClickListener {
            Log.d(TAG, "$LOG_PREFIX Texto de registro clickeado, cambiando modo")
            toggleRegisterMode()
        }

        binding.tvForgotPassword.setOnClickListener {
            Log.d(TAG, "$LOG_PREFIX Olvidé contraseña clickeado")
            showSnackbar("Funcionalidad de recuperación en desarrollo")
        }
    }

    private var isRegisterMode = false

    private fun toggleRegisterMode() {
        isRegisterMode = !isRegisterMode
        Log.d(TAG, "$LOG_PREFIX Cambiando modo a: ${if (isRegisterMode) "Registro" else "Login"}")

        if (isRegisterMode) {
            binding.tilName.visibility = View.VISIBLE
            binding.btnLogin.text = "Registrarse"
            binding.tvRegister.text = "¿Ya tienes cuenta? Iniciar sesión"
            binding.toolbar.title = "Registrarse"
            // Mostrar requisitos iniciales
            binding.tilPassword.helperText = "Requisitos: 8+ caracteres, mayúscula, minúscula, número y símbolo"
            Log.d(TAG, "$LOG_PREFIX UI actualizada para modo registro")
        } else {
            binding.tilName.visibility = View.GONE
            binding.btnLogin.text = "Iniciar sesión"
            binding.tvRegister.text = "¿No tienes cuenta? Regístrate"
            binding.toolbar.title = "Iniciar sesión"
            binding.tilPassword.error = null
            binding.tilPassword.helperText = null
            Log.d(TAG, "$LOG_PREFIX UI actualizada para modo login")
        }
    }

    private fun validateInputs(): Boolean {
        Log.d(TAG, "$LOG_PREFIX Validando inputs en modo ${if (isRegisterMode) "registro" else "login"}")
        var isValid = true
        var firstErrorView: View? = null

        with(binding) {
            // Validar nombre si está visible (registro)
            if (tilName.visibility == View.VISIBLE) {
                val name = etName.text.toString().trim()
                if (name.isEmpty()) {
                    Log.d(TAG, "$LOG_PREFIX Validación fallida: nombre vacío")
                    tilName.error = "Nombre requerido"
                    firstErrorView = firstErrorView ?: etName
                    isValid = false
                } else {
                    tilName.error = null
                }
            }

            // Validar correo
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Log.d(TAG, "$LOG_PREFIX Validación fallida: email vacío")
                tilEmail.error = "Correo requerido"
                firstErrorView = firstErrorView ?: etEmail
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Log.d(TAG, "$LOG_PREFIX Validación fallida: email no válido")
                tilEmail.error = "Correo no válido"
                firstErrorView = firstErrorView ?: etEmail
                isValid = false
            } else {
                tilEmail.error = null
            }

            // Validar contraseña
            val password = etPassword.text.toString()
            if (password.isEmpty()) {
                Log.d(TAG, "$LOG_PREFIX Validación fallida: contraseña vacía")
                tilPassword.error = "Contraseña requerida"
                firstErrorView = firstErrorView ?: etPassword
                isValid = false
            } else if (isRegisterMode) {
                val requirements = checkPasswordRequirements(password)
                if (!requirements.all { it.value }) {
                    Log.d(TAG, "$LOG_PREFIX Validación fallida: contraseña no cumple requisitos")
                    tilPassword.error = "La contraseña no cumple con todos los requisitos"
                    firstErrorView = firstErrorView ?: etPassword
                    isValid = false
                } else {
                    tilPassword.error = null
                }
            }
        }

        firstErrorView?.requestFocus()

        if (!isValid) {
            Log.w(TAG, "$LOG_PREFIX Validación general fallida")
            Snackbar.make(binding.root, "Corrige los errores antes de continuar", Snackbar.LENGTH_LONG).show()
        }

        return isValid
    }

    private fun loginUser(email: String, password: String) {
        Log.i(TAG, "$LOG_PREFIX Iniciando proceso de login para: $email")
        showLoading(true)

        lifecycleScope.launch {
            try {
                Log.d(TAG, "$LOG_PREFIX Creando request de login")
                val request = LoginRequest(email, password)
                val api = ApiClient.getClientWithoutToken(this@MainActivity)
                val response = api.login(request)

                if (response.isSuccessful) {
                    val token = response.body()?.access_token ?: ""
                    val name = response.body()?.user?.name ?: "Usuario"
                    val user = response.body()?.user
                    Log.i(TAG, "$LOG_PREFIX Login exitoso para: $email")
                    Log.d(TAG, "$LOG_PREFIX Token recibido: ${if (token.isEmpty()) "vacío" else "***"}")
                    Log.d(TAG, "$LOG_PREFIX Datos usuario: $user")

                    // Guardar en SharedPreferences
                    with(sharedPrefs.edit()) {
                        putString("token", token)
                        putInt("user_id", user?.id ?: -1)
                        putString("name", user?.name ?: "")
                        putString("email", user?.email ?: "")
                        putString("telefono", user?.telefono ?: "")
                        putString("direccion", user?.direccion ?: "")
                        putString("email_verified_at", user?.email_verified_at ?: "")
                        putString("role", user?.role ?: "usuario")
                        apply()
                        Log.d(TAG, "$LOG_PREFIX SharedPreferences Login inicializado correctamente")
                    }
                    Log.d(TAG, "$LOG_PREFIX Datos guardados en SharedPreferences")

                    showSnackbar("Inicio de sesión exitoso")
                    Log.d("LOGIN", "Token: $token")

                    val intent = Intent(this@MainActivity, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    val errorMessage = response.errorBody()?.string() ?: "Credenciales incorrectas"
                    Log.e(TAG, "$LOG_PREFIX Error en login - Código: ${response.code()}, Mensaje: $errorMessage")
                    showSnackbar(errorMessage)
                    Log.e("LOGIN_ERROR", "Error: $errorMessage")
                }
            } catch (e: Exception) {
                Log.e(TAG, "$LOG_PREFIX Excepción en login: ${e.message}", e)
                showSnackbar("Error de conexión: ${e.message}")
                Log.e("LOGIN_EXCEPTION", "Exception: ${e.message}", e)
            } finally {
                Log.d(TAG, "$LOG_PREFIX Finalizando proceso de login")
                showLoading(false)
            }
        }
    }

    private fun registerUser(name: String, email: String, password: String) {
        Log.i(TAG, "$LOG_PREFIX Iniciando proceso de registro para: $email")
        showLoading(true)

        lifecycleScope.launch {
            try {
                Log.d(TAG, "$LOG_PREFIX Creando request de registro")
                val request = RegisterRequest(name, email, password)
                val api = ApiClient.getClientWithoutToken(this@MainActivity)
                val response = api.register(request)

                if (response.isSuccessful) {
                    val token = response.body()?.access_token ?: ""
                    Log.i(TAG, "$LOG_PREFIX Registro exitoso para: $email")
                    Log.d(TAG, "$LOG_PREFIX Token recibido: ${if (token.isEmpty()) "vacío" else "***"}")

                    // Guardar en SharedPreferences
                    with(sharedPrefs.edit()) {
                        putString("token", token)
                        putString("name", name)
                        putString("email", email)
                        apply()
                        Log.d(TAG, "$LOG_PREFIX SharedPreferences Registro inicializado correctamente")
                    }



                    showSnackbar("Registro exitoso")
                    Log.d("REGISTER", "Token: $token")

                    val intent = Intent(this@MainActivity, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)

                } else {
                    val errorBody = response.errorBody()?.string() ?: "Error al registrar"
                    Log.e(TAG, "$LOG_PREFIX Error en registro - Código: ${response.code()}, Mensaje: $errorBody")
                    showSnackbar(errorBody)
                    Log.e("REGISTER_ERROR", "Código: ${response.code()}, Error: $errorBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "$LOG_PREFIX Excepción en registro: ${e.message}", e)
                showSnackbar("Error de conexión: ${e.message}")
                Log.e("REGISTER_EXCEPTION", "Excepción: ${e.message}", e)
            } finally {
                showLoading(false)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        Log.v(TAG, "$LOG_PREFIX Actualizando estado loading: $isLoading")
        binding.btnLogin.isEnabled = !isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showSnackbar(message: String) {
        Log.d(TAG, "$LOG_PREFIX Mostrando Snackbar: $message")
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }
}