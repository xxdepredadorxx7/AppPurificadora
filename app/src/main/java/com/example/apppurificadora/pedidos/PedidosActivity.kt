package com.example.apppurificadora.pedidos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.apppurificadora.databinding.ActivityPedidosBinding
import com.example.apppurificadora.models.PedidoResponse
import com.example.apppurificadora.api.ApiClient
import com.example.apppurificadora.api.PrefsHelper
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PedidosActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPedidosBinding
    private lateinit var adapter: PedidoAdapter
    private val pedidos = mutableListOf<PedidoResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("LocPedidos", "onCreate iniciado")

        binding = ActivityPedidosBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("LocPedidos", "Binding y layout establecidos")

        val baseUrl = PrefsHelper.getBaseUrl(this)
        Log.d("BaseUrlTracker", "Base URL actual: $baseUrl")

        adapter = PedidoAdapter(pedidos)
        binding.recyclerViewPedidos.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewPedidos.adapter = adapter
        Log.d("LocPedidos", "RecyclerView configurado")

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Pedidos"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        Log.d("LocPedidos", "Toolbar configurado")

        binding.toolbar.setNavigationOnClickListener {
            Log.d("LocPedidos", "Botón de retroceso presionado")
            onBackPressedDispatcher.onBackPressed()
        }

        cargarPedidos()
    }

    private fun cargarPedidos() {
        Log.d("LocPedidos", "Iniciando carga de pedidos")
        val token = PrefsHelper.getToken(this)
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Token no encontrado", Toast.LENGTH_SHORT).show()
            Log.d("LocPedidos", "Token no encontrado")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val api = ApiClient.getClient(this@PedidosActivity)
                val response = api.getPedidos()
                Log.d("LocPedidos", "Respuesta obtenida del API")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        pedidos.clear()
                        response.body()?.let {
                            pedidos.addAll(it)
                            Log.d("LocPedidos", "Pedidos cargados exitosamente: ${pedidos.size}")
                        }
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this@PedidosActivity, "Error al cargar pedidos", Toast.LENGTH_SHORT).show()
                        Log.d("LocPedidos", "Error al cargar pedidos: código ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PedidosActivity, "Error de red: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("LocPedidos", "Error de red al cargar pedidos", e)
                }
            }
        }
    }
}
