package com.example.apppurificadora

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.apppurificadora.api.ApiClient
import com.example.apppurificadora.api.PrefsHelper
import com.example.apppurificadora.databinding.ActivityHomeBinding
import com.example.apppurificadora.pedidos.PedidosActivity
import com.example.apppurificadora.products.ProductosActivity
import com.example.apppurificadora.profile.EditProfileActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HomeActivity"
        private const val LOG_PREFIX = "🏠 [Home]"
    }

    private lateinit var binding: ActivityHomeBinding
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "$LOG_PREFIX onCreate() - Iniciando actividad principal")

        val baseUrl = PrefsHelper.getBaseUrl(this)
        Log.d("BaseUrlTracker", "Base URL actual: $baseUrl")

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialización de SharedPreferences
        sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        Log.d(TAG, "$LOG_PREFIX SharedPreferences inicializado")

        // Cargar y mostrar datos del usuario
        val userName = sharedPrefs.getString("name", "Usuario").also {
            Log.d(TAG, "$LOG_PREFIX Nombre de usuario obtenido: $it")
        }
        val userEmail = sharedPrefs.getString("email", "usuario@email.com").also {
            Log.d(TAG, "$LOG_PREFIX Email de usuario obtenido: $it")
        }

        binding.tvWelcome.text = "Bienvenido, $userName 👋"
        binding.tvEmail.text = userEmail
        Log.i(TAG, "$LOG_PREFIX UI actualizada con datos de usuario")

        // Configuración de listeners
        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        Log.d(TAG, "$LOG_PREFIX Configurando listeners de botones")

        binding.btnProductos.setOnClickListener {
            Log.i(TAG, "$LOG_PREFIX Botón Productos clickeado")
            startActivity(Intent(this, ProductosActivity::class.java)).also {
                Log.d(TAG, "$LOG_PREFIX Navegando a ProductosActivity")
            }
        }

        binding.btnPedidos.setOnClickListener {
            Log.i(TAG, "$LOG_PREFIX Botón Pedidos clickeado")
            startActivity(Intent(this, PedidosActivity::class.java)).also {
                Log.d(TAG, "$LOG_PREFIX Navegando a PedidosActivity")
            }
        }

        binding.btnEditProfile.setOnClickListener {
            Log.i(TAG, "$LOG_PREFIX Botón Editar Perfil clickeado")
            startActivity(Intent(this, EditProfileActivity::class.java)).also {
                Log.d(TAG, "$LOG_PREFIX Navegando a EditProfileActivity")
            }
        }

        binding.btnLogout.setOnClickListener {
            Log.i(TAG, "$LOG_PREFIX Botón Cerrar Sesión clickeado")
            mostrarConfirmacion()
        }
    }

    private fun mostrarConfirmacion() {
        Log.d(TAG, "$LOG_PREFIX Mostrando diálogo de confirmación de cierre de sesión")

        AlertDialog.Builder(this)
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que quieres cerrar sesión?")
            .setPositiveButton("Sí") { _: DialogInterface, _: Int ->
                Log.d(TAG, "$LOG_PREFIX Usuario confirmó cierre de sesión")
                cerrarSesion()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Log.d(TAG, "$LOG_PREFIX Usuario canceló cierre de sesión")
            }
            .show().also {
                Log.v(TAG, "$LOG_PREFIX Diálogo de confirmación mostrado")
            }
    }

    private fun cerrarSesion() {
        Log.i(TAG, "$LOG_PREFIX Procediendo a cerrar sesión")

        // Limpiar preferencias
        sharedPrefs.edit().clear().apply().also {
            Log.d(TAG, "$LOG_PREFIX SharedPreferences limpiado")
        }

        // Mostrar feedback
        Toast.makeText(this, "Sesión cerrada exitosamente", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "$LOG_PREFIX Toast de cierre de sesión mostrado")

        // Redirigir a MainActivity
        Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(this)
            Log.d(TAG, "$LOG_PREFIX Redirigiendo a MainActivity")
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "$LOG_PREFIX onResume() - Actualizando datos de usuario")

        val userName = sharedPrefs.getString("name", "Usuario").also {
            Log.v(TAG, "$LOG_PREFIX Nombre de usuario actual: $it")
        }
        val userEmail = sharedPrefs.getString("email", "usuario@email.com").also {
            Log.v(TAG, "$LOG_PREFIX Email de usuario actual: $it")
        }

        binding.tvWelcome.text = "Bienvenido, $userName 👋"
        binding.tvEmail.text = userEmail
        Log.d(TAG, "$LOG_PREFIX UI actualizada con datos actualizados")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "$LOG_PREFIX onDestroy() - Actividad siendo destruida")
    }
}