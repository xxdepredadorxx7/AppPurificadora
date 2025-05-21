package com.example.apppurificadora.pedidos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.apppurificadora.databinding.ItemPedidoBinding
import com.example.apppurificadora.models.PedidoResponse

class PedidoAdapter(private val pedidos: List<PedidoResponse>) :
    RecyclerView.Adapter<PedidoAdapter.PedidoViewHolder>() {

    inner class PedidoViewHolder(private val binding: ItemPedidoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pedido: PedidoResponse) {
            binding.tvProductoNombre.text = pedido.producto.nombre
            binding.tvCantidad.text = "Cantidad: ${pedido.cantidad}"
            binding.tvTotal.text = "Total: $${pedido.total}"
            binding.tvEstado.text = "Estado: ${pedido.estado}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PedidoViewHolder {
        val binding = ItemPedidoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PedidoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PedidoViewHolder, position: Int) {
        holder.bind(pedidos[position])
    }

    override fun getItemCount(): Int = pedidos.size
}
