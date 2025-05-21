package com.example.apppurificadora.profile

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.apppurificadora.MainActivity
import com.example.apppurificadora.api.ApiClient
import com.example.apppurificadora.api.PrefsHelper
import com.example.apppurificadora.databinding.ActivityEditProfileBinding
import com.example.apppurificadora.models.UpdateProfileRequest
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditProfileActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "EditProfileActivity"
        private const val LOG_PREFIX = "👤 [EditProfile]"
    }

    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "$LOG_PREFIX onCreate() - Iniciando actividad de edición de perfil")
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val baseUrl = PrefsHelper.getBaseUrl(this)
        Log.d("BaseUrlTracker", "Base URL actual: $baseUrl")

        sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        Log.d(TAG, "$LOG_PREFIX SharedPreferences inicializado")

        setupToolbar()
        loadUserData()
        setupListeners()
    }

    private fun setupToolbar() {
        Log.d(TAG, "$LOG_PREFIX Configurando toolbar")
        binding.toolbar.setNavigationOnClickListener {
            Log.d(TAG, "$LOG_PREFIX Botón de navegación clickeado")
            onBackPressed()
        }
    }

    private fun loadUserData() {
        Log.d(TAG, "$LOG_PREFIX Cargando datos del usuario desde SharedPreferences")
        binding.apply {
            // Cargar datos básicos
            etName.setText(sharedPrefs.getString("name", ""))
            etEmail.setText(sharedPrefs.getString("email", ""))
            etTelefono.setText(sharedPrefs.getString("telefono", ""))
            etDireccion.setText(sharedPrefs.getString("direccion", ""))

            // Configurar estado de verificación de email
            val isEmailVerified = !sharedPrefs.getString("email_verified_at", "").isNullOrEmpty()
            Log.d(TAG, "$LOG_PREFIX Email verificado: $isEmailVerified")
            tvEmailVerified.text = if (isEmailVerified) "✓ Email verificado" else "⚠ Email no verificado"
            tvEmailVerified.setTextColor(if (isEmailVerified) Color.GREEN else Color.RED)
            }
    }

    private fun setupListeners() {
        Log.d(TAG, "$LOG_PREFIX Configurando listeners")
        binding.btnSave.setOnClickListener {
            Log.d(TAG, "$LOG_PREFIX Botón Guardar clickeado")
            if (validateInputs()) {
                updateProfile()
            }else {
                Log.d(TAG, "$LOG_PREFIX Validación fallida, no se procede con actualización")
            }
        }

        binding.btnCancel.setOnClickListener {
            Log.d(TAG, "$LOG_PREFIX Botón Cancelar clickeado")
            onBackPressed()
        }

        // Validación en tiempo real de la contraseña
        binding.etNewPassword.addTextChangedListener(passwordTextWatcher)
        Log.d(TAG, "$LOG_PREFIX Listener de contraseña configurado")
    }

    private val passwordTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            val password = s?.toString() ?: ""
            Log.v(TAG, "$LOG_PREFIX Texto cambiado en campo contraseña")
            if (password.isNotEmpty()) {
                Log.d(TAG, "$LOG_PREFIX Validando requisitos de contraseña")
                showPasswordRequirements(password)
            } else {
                binding.tilNewPassword.error = null
                binding.tilNewPassword.helperText = null
            }
        }
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
            binding.tilNewPassword.error = null
            binding.tilNewPassword.helperText = "✓ Contraseña válida"
            binding.tilNewPassword.setHelperTextColor(ColorStateList.valueOf(Color.GREEN))
        } else {
            Log.d(TAG, "$LOG_PREFIX Contraseña no cumple requisitos: ${messages.joinToString(", ")}")
            binding.tilNewPassword.helperText = "Requisitos:"
            binding.tilNewPassword.error = messages.joinToString("\n")
        }
    }

    private fun validateInputs(): Boolean {
        Log.d(TAG, "$LOG_PREFIX Validando inputs del formulario")
        var isValid = true
        var firstErrorView: View? = null

        with(binding) {
            // Nombre
            val name = etName.text.toString().trim()
            if (name.isEmpty()) {
                Log.d(TAG, "$LOG_PREFIX Validación fallida: nombre vacío")
                tilName.error = "El nombre es obligatorio"
                firstErrorView = firstErrorView ?: etName
                isValid = false
            } else {
                tilName.error = null
            }

            // Teléfono
            val telefono = etTelefono.text.toString().trim()
            if (telefono.isNotEmpty() && !telefono.matches(Regex("^\\d{10}$"))) {
                Log.d(TAG, "$LOG_PREFIX Validación fallida: teléfono no válido")
                tilTelefono.error = "Debe contener 10 dígitos numéricos"
                firstErrorView = firstErrorView ?: etTelefono
                isValid = false
            } else {
                tilTelefono.error = null
            }

            // Dirección (no se valida por ahora)
            tilDireccion.error = null

            // Contraseñas
            val currentPassword = etCurrentPassword.text.toString()
            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            val isChangingPassword = currentPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmPassword.isNotEmpty()
            Log.d(TAG, "$LOG_PREFIX Cambiando contraseña: $isChangingPassword")
            if (isChangingPassword) {
                if (currentPassword.isEmpty()) {
                    Log.d(TAG, "$LOG_PREFIX Validación fallida: contraseña actual vacía")
                    tilCurrentPassword.error = "Ingrese su contraseña actual"
                    firstErrorView = firstErrorView ?: etCurrentPassword
                    isValid = false
                } else {
                    tilCurrentPassword.error = null
                }

                if (newPassword.isEmpty()) {
                    Log.d(TAG, "$LOG_PREFIX Validación fallida: nueva contraseña vacía")
                    tilNewPassword.error = "Ingrese una nueva contraseña"
                    firstErrorView = firstErrorView ?: etNewPassword
                    isValid = false
                } else {
                    val requirements = checkPasswordRequirements(newPassword)
                    if (!requirements.all { it.value }) {
                        Log.d(TAG, "$LOG_PREFIX Validación fallida: contraseña no cumple requisitos")
                        tilNewPassword.error = "La contraseña no cumple con todos los requisitos"
                        firstErrorView = firstErrorView ?: etNewPassword
                        isValid = false
                    } else {
                        tilNewPassword.error = null
                        tilNewPassword.helperText = "Contraseña válida"
                        tilNewPassword.setHelperTextColor(ColorStateList.valueOf(Color.GREEN))
                    }
                }

                if (confirmPassword != newPassword) {
                    Log.d(TAG, "$LOG_PREFIX Validación fallida: contraseñas no coinciden")
                    tilConfirmPassword.error = "Las contraseñas no coinciden"
                    firstErrorView = firstErrorView ?: etConfirmPassword
                    isValid = false
                } else {
                    tilConfirmPassword.error = null
                }
            } else {
                tilCurrentPassword.error = null
                tilNewPassword.error = null
                tilConfirmPassword.error = null
            }
        }

        // Enfocar primer campo con error
        firstErrorView?.requestFocus()

        // Mostrar mensaje general
        if (!isValid) {
            Log.w(TAG, "$LOG_PREFIX Validación general fallida")
            Snackbar.make(binding.root, "Por favor corrige los errores antes de continuar", Snackbar.LENGTH_LONG).show()
        }else {
            Log.d(TAG, "$LOG_PREFIX Validación exitosa")
        }

        return isValid
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

    private fun updateProfile() {
        Log.i(TAG, "$LOG_PREFIX Iniciando actualización de perfil")
        showLoading(true)

        val savedToken = sharedPrefs.getString("token", null)
        val savedUserId = sharedPrefs.getInt("user_id", -1)
        Log.e(TAG, "$LOG_PREFIX ERROR: ID de usuario no válido")
        Log.d("EDIT_PROFILE", "Token guardado: $savedToken")
        Log.d("EDIT_PROFILE", "UserId guardado: $savedUserId")

        val userId = sharedPrefs.getInt("user_id", -1).takeIf { it != -1 }
            ?: run {
                Toast.makeText(this, "Error: No se encontró ID de usuario", Toast.LENGTH_LONG).show()
                showLoading(false)
                return
            }

        val token = sharedPrefs.getString("token", null)
        Log.d(TAG, "$LOG_PREFIX Token recuperado: ${token?.take(10)}...") // Log parcial por seguridad
        Log.d("TOKEN", "Token recuperado: $token")

        if (token.isNullOrBlank()) {
            Log.e(TAG, "$LOG_PREFIX ERROR: Token de autenticación vacío o nulo")
            Toast.makeText(this, "Error: No hay token de autenticación válido", Toast.LENGTH_LONG).show()
            showLoading(false)
            Log.e("EDIT_PROFILE", "Token de autenticación vacío o nulo, cancelando actualización.")
            return
        }

        val authHeader = "Bearer $token"
        Log.d(TAG, "$LOG_PREFIX Authorization header preparado")
        Log.d("EDIT_PROFILE", "Authorization header preparado: $authHeader")

        val request = UpdateProfileRequest(
            name = binding.etName.text.toString(),
            direccion = binding.etDireccion.text?.toString(),
            telefono = binding.etTelefono.text?.toString(),
            currentPassword = binding.etCurrentPassword.text?.toString(),
            newPassword = binding.etNewPassword.text?.toString(),
            confirmPassword = binding.etConfirmPassword.text?.toString()
        )
        Log.d(TAG, "$LOG_PREFIX Datos de actualización: ${
            request.copy(
                currentPassword = request.currentPassword?.let { "***" },
                newPassword = request.newPassword?.let { "***" },
                confirmPassword = request.confirmPassword?.let { "***" }
            )
        }")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "$LOG_PREFIX Realizando petición de actualización")
                Log.d("EDIT_PROFILE", "Iniciando actualización del perfil para userId=$userId")
                val api = ApiClient.getClient(this@EditProfileActivity)
                val response = api.updateProfile(
                    id = userId,
                    request = request
                )

                withContext(Dispatchers.Main) {
                    when {
                        response.isSuccessful -> {
                            Log.i(TAG, "$LOG_PREFIX Perfil actualizado correctamente")
                            Log.d("EDIT_PROFILE", "Perfil actualizado correctamente: ${response.body()}")
                            response.body()?.let { authResponse ->
                                sharedPrefs.edit().apply {
                                    putString("name", authResponse.user.name)
                                    putString("direccion", authResponse.user.direccion)
                                    putString("telefono", authResponse.user.telefono)
                                    putString("email", authResponse.user.email)
                                    putString("email_verified_at", authResponse.user.email_verified_at)
                                    putString("role", authResponse.user.role)
                                    apply()
                                }
                                Log.d(TAG, "$LOG_PREFIX Datos guardados en SharedPreferences")
                                Toast.makeText(
                                    this@EditProfileActivity,
                                    "Perfil actualizado correctamente",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                                Log.w(TAG, "$LOG_PREFIX Respuesta exitosa pero cuerpo vacío")
                            }
                        }

                        response.code() == 302 -> {
                            Log.e(TAG, "$LOG_PREFIX ERROR 302: Token inválido o sesión expirada")
                            Log.e("EDIT_PROFILE", "Token inválido o sesión expirada (302 Found)")
                            Toast.makeText(
                                this@EditProfileActivity,
                                "Sesión expirada. Por favor, inicia sesión nuevamente.",
                                Toast.LENGTH_LONG
                            ).show()
                            cerrarSesionYRedirigir()
                        }

                        else -> {
                            val rawErrorBody = response.errorBody()?.string()
                            Log.e(TAG, "$LOG_PREFIX ERROR ${response.code()}: $rawErrorBody")
                            Log.e("EDIT_PROFILE", "Respuesta cruda del servidor: $rawErrorBody")
                            val errorMsg = rawErrorBody ?: "Error al actualizar"
                            Log.e("EDIT_PROFILE", "Error en respuesta: $errorMsg")
                            Toast.makeText(
                                this@EditProfileActivity,
                                errorMsg,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "$LOG_PREFIX Excepción al actualizar perfil", e)
                    Log.e("EDIT_PROFILE", "Error de red: ${e.message}", e)
                    Toast.makeText(
                        this@EditProfileActivity,
                        "Error de conexión: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "$LOG_PREFIX Finalizando proceso de actualización")
                    showLoading(false)
                    Log.d("EDIT_PROFILE", "Finalizó actualización del perfil")
                }
            }
        }
    }

    private fun cerrarSesionYRedirigir() {
        Log.i(TAG, "$LOG_PREFIX Cerrando sesión y redirigiendo a login")
        sharedPrefs.edit().clear().apply()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showLoading(loading: Boolean) {
        Log.v(TAG, "$LOG_PREFIX Actualizando estado de carga: $loading")
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
        binding.btnCancel.isEnabled = !loading
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "$LOG_PREFIX onDestroy() - Actividad siendo destruida")
    }
}