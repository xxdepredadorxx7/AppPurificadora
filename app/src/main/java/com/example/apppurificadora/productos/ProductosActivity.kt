package com.example.apppurificadora.products

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.apppurificadora.api.ApiClient
import com.example.apppurificadora.api.PrefsHelper
import com.example.apppurificadora.databinding.ActivityProductosBinding
import com.example.apppurificadora.models.PedidoRequest
import com.example.apppurificadora.models.ProductoRequest
import com.example.apppurificadora.pedidos.PedidosActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class ProductosActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ProductosActivity"
        private const val LOG_PREFIX = "📦 [Productos]"
    }

    private lateinit var binding: ActivityProductosBinding
    private lateinit var sharedPrefs: SharedPreferences
    private var productosList: List<ProductoRequest> = emptyList()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "$LOG_PREFIX onCreate() - Iniciando actividad de productos")
        binding = ActivityProductosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val baseUrl = PrefsHelper.getBaseUrl(this)
        Log.d("BaseUrlTracker", "Base URL actual: $baseUrl")

        sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        currencyFormat.maximumFractionDigits = 2

        Log.d(TAG, "$LOG_PREFIX Configurando preferencias y formato de moneda")
        setupToolbar()
        setupListeners()

        Log.i(TAG, "$LOG_PREFIX Cargando lista de productos")
        loadProductos()
    }

    private fun setupToolbar() {
        Log.d(TAG, "$LOG_PREFIX Configurando toolbar")
        binding.toolbar.setNavigationOnClickListener {
            Log.d(TAG, "$LOG_PREFIX Botón de navegación clickeado")
            onBackPressed()
        }
    }

    private fun setupListeners() {
        Log.d(TAG, "$LOG_PREFIX Configurando listeners de botones")

        binding.btnPedidoRelleno.setOnClickListener {
            Log.d(TAG, "$LOG_PREFIX Botón de relleno clickeado")
            productosList.firstOrNull { it.nombre == "Relleno de agua" }?.let { producto ->
                Log.i(TAG, "$LOG_PREFIX Producto 'Relleno de agua' encontrado (ID: ${producto.id})")
                showQuantityDialog(producto)
            } ?: run {
                Log.w(TAG, "$LOG_PREFIX Producto 'Relleno de agua' no encontrado en la lista")
                showError("Producto no disponible")
            }
        }

        binding.btnPedidoGarrafon.setOnClickListener {
            Log.d(TAG, "$LOG_PREFIX Botón de garrafón clickeado")
            productosList.firstOrNull { it.nombre == "Garrafón 20L Nuevo" }?.let { producto ->
                Log.i(TAG, "$LOG_PREFIX Producto 'Garrafón 20L Nuevo' encontrado (ID: ${producto.id})")
                showQuantityDialog(producto)
            } ?: run {
                Log.w(TAG, "$LOG_PREFIX Producto 'Garrafón 20L Nuevo' no encontrado en la lista")
                showError("Producto no disponible")
            }
        }
    }

    private fun showQuantityDialog(productoRequest: ProductoRequest) {
        val maxQuantity = productoRequest.cantidad.coerceAtMost(10)
        Log.d(TAG, "$LOG_PREFIX Mostrando diálogo de cantidad para ${productoRequest.nombre}. Máx: $maxQuantity")

        if (maxQuantity <= 0) {
            Log.w(TAG, "$LOG_PREFIX Producto ${productoRequest.nombre} sin stock disponible")
            showError("No hay stock disponible")
            return
        }

        val quantities = (1..maxQuantity).toList().toTypedArray()
        Log.v(TAG, "$LOG_PREFIX Cantidades disponibles: ${quantities.joinToString()}")

        MaterialAlertDialogBuilder(this)
            .setTitle("Seleccionar cantidad")
            .setItems(quantities.map { it.toString() }.toTypedArray()) { _, which ->
                val selectedQuantity = quantities[which]
                Log.i(TAG, "$LOG_PREFIX Cantidad seleccionada: $selectedQuantity para ${productoRequest.nombre}")
                confirmPedido(productoRequest, selectedQuantity)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Log.d(TAG, "$LOG_PREFIX Diálogo de cantidad cancelado")
            }
            .show()
    }

    private fun confirmPedido(productoRequest: ProductoRequest, cantidad: Int) {
        val total = productoRequest.precio * cantidad
        Log.i(TAG, "$LOG_PREFIX Confirmando pedido - Producto: ${productoRequest.nombre}, Cantidad: $cantidad, Total: $total")

        val message = """
            Producto: ${productoRequest.nombre}
            Cantidad: $cantidad
            Precio unitario: ${currencyFormat.format(productoRequest.precio)}
            Total: ${currencyFormat.format(total)}
            
            ¿Confirmar pedido?
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar Pedido")
            .setMessage(message)
            .setPositiveButton("Confirmar") { _, _ ->
                Log.i(TAG, "$LOG_PREFIX Pedido confirmado por el usuario")
                hacerPedido(productoRequest, cantidad, total)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Log.d(TAG, "$LOG_PREFIX Pedido cancelado por el usuario")
            }
            .show()
    }

    private fun loadProductos() {
        Log.i(TAG, "$LOG_PREFIX URL base actual: ${PrefsHelper.getBaseUrl(this)}")
        Log.i(TAG, "$LOG_PREFIX Iniciando carga de productos desde API")
        val token = PrefsHelper.getToken(this)
        Log.d(TAG, "$LOG_PREFIX Token JWT presente: ${!token.isNullOrEmpty()}")
        Log.v(TAG, "$LOG_PREFIX Token: ${token?.take(10)}...")
        showLoading(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "$LOG_PREFIX Realizando petición a API para obtener productos")
                val api = ApiClient.getClient(this@ProductosActivity)
                val response = api.getProductos()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        productosList = response.body() ?: emptyList()
                        Log.i(TAG, "$LOG_PREFIX Productos cargados exitosamente. Recibidos: ${productosList.size} productos")
                        updateUIWithProductos()
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Error desconocido"
                        Log.e(TAG, "$LOG_PREFIX Error al cargar productos. Código: ${response.code()}, Mensaje: $errorMsg")
                        showError("Error al cargar productos")
                        loadSampleProducts()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "$LOG_PREFIX Excepción al cargar productos: ${e.message}", e)
                    showError("Error de conexión")
                    loadSampleProducts()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "$LOG_PREFIX Finalizando carga de productos")
                    showLoading(false)
                }
            }
        }
    }

    private fun loadSampleProducts() {
        Log.w(TAG, "$LOG_PREFIX Cargando productos de muestra (modo offline)")
        productosList = listOf(
            ProductoRequest(
                id = 1,
                nombre = "Relleno de agua",
                descripcion = "Relleno de garrafón de 20 litros",
                precio = 15.0,
                cantidad = 300
            ),
            ProductoRequest(
                id = 2,
                nombre = "Garrafón 20L Nuevo",
                descripcion = "Garrafón de agua de 20 litros",
                precio = 35.0,
                cantidad = 34
            )
        )
        updateUIWithProductos()
    }

    private fun updateUIWithProductos() {
        Log.d(TAG, "$LOG_PREFIX Actualizando UI con lista de productos")

        productosList.firstOrNull { it.nombre == "Relleno de agua" }?.let { producto ->
            binding.apply {
                btnPedidoRelleno.isEnabled = producto.cantidad > 0
                btnPedidoRelleno.text = if (producto.cantidad > 0) "Hacer pedido" else "AGOTADO"
                tvCantidadRelleno.text = "Cantidad Disponible: ${producto.cantidad}"
            }
            Log.d(TAG, "$LOG_PREFIX Estado Relleno: ${if (producto.cantidad > 0) "Disponible (${producto.cantidad})" else "Agotado"}")
        } ?: Log.w(TAG, "$LOG_PREFIX Producto 'Relleno de agua' no encontrado para actualizar UI")

        productosList.firstOrNull { it.nombre == "Garrafón 20L Nuevo" }?.let { producto ->
            binding.apply {
                btnPedidoGarrafon.isEnabled = producto.cantidad > 0
                btnPedidoGarrafon.text = if (producto.cantidad > 0) "Hacer pedido" else "AGOTADO"
                tvCantidadGarrafon.text = "Cantidad Disponible: ${producto.cantidad}"
            }
            Log.d(TAG, "$LOG_PREFIX Estado Garrafón: ${if (producto.cantidad > 0) "Disponible (${producto.cantidad})" else "Agotado"}")
        } ?: Log.w(TAG, "$LOG_PREFIX Producto 'Garrafón 20L Nuevo' no encontrado para actualizar UI")
    }


    private fun hacerPedido(productoRequest: ProductoRequest, cantidad: Int, total: Double) {
        Log.i(TAG, "$LOG_PREFIX Preparando pedido - ProductoID: ${productoRequest.id}, Cantidad: $cantidad, Total: $total")
        showLoading(true)

        val userId = sharedPrefs.getInt("user_id", -1).takeIf { it != -1 } ?: run {
            Log.e(TAG, "$LOG_PREFIX Error: ID de usuario no válido")
            showError("Usuario no identificado")
            showLoading(false)
            return
        }

        val pedidoRequest = PedidoRequest(
            producto_id = productoRequest.id,
            cantidad = cantidad,
            total = total
        )

        Log.d(TAG, "$LOG_PREFIX Datos completos del pedido: $pedidoRequest")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.i(TAG, "$LOG_PREFIX Enviando pedido a la API")
                val api = ApiClient.getClient(this@ProductosActivity)
                val response = api.crearPedido(pedidoRequest)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        response.body()?.let { pedido ->
                            Log.i(TAG, "$LOG_PREFIX Pedido creado exitosamente. ID: ${pedido.id}")
                            showSuccess("Pedido #${pedido.id} realizado")
                            loadProductos() // Recargar productos para actualizar stock
                            val intent = Intent(this@ProductosActivity, PedidosActivity::class.java)
                            startActivity(intent)
                            finish()

                        } ?: run {
                            Log.w(TAG, "$LOG_PREFIX Pedido creado pero respuesta vacía del servidor")
                            showError("Pedido realizado pero sin confirmación")
                        }
                    } else {
                        val errorMsg = response.errorBody()?.string() ?: "Error sin mensaje"
                        Log.e(TAG, "$LOG_PREFIX Error en la API. Código: ${response.code()}, Mensaje: $errorMsg")
                        showError("Error al procesar pedido")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "$LOG_PREFIX Excepción al realizar pedido: ${e.message}", e)
                    showError("Error de conexión al realizar pedido")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    Log.d(TAG, "$LOG_PREFIX Finalizando proceso de pedido")
                    showLoading(false)
                }
            }
        }
    }

    private fun showError(message: String) {
        Log.e(TAG, "$LOG_PREFIX Mostrando error al usuario: $message")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Log.i(TAG, "$LOG_PREFIX Mostrando éxito al usuario: $message")
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showLoading(loading: Boolean) {
        Log.v(TAG, "$LOG_PREFIX Actualizando estado de carga: $loading")
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnPedidoRelleno.isEnabled = !loading && (productosList.firstOrNull { it.nombre == "Relleno de agua" }?.cantidad ?: 0) > 0
        binding.btnPedidoGarrafon.isEnabled = !loading && (productosList.firstOrNull { it.nombre == "Garrafón 20L Nuevo" }?.cantidad ?: 0) > 0
    }
}