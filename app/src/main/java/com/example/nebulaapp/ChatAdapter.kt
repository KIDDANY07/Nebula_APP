package com.example.nebulaapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class ChatAdapter(
    private val mensajes: List<Mensaje>
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mensaje, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val mensaje = mensajes[position]
        holder.bind(mensaje)
    }

    override fun getItemCount() = mensajes.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvContenido: TextView = itemView.findViewById(R.id.tvContenido)
        private val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        private val tvEmisor: TextView = itemView.findViewById(R.id.tvEmisor)

        fun bind(mensaje: Mensaje) {
            tvContenido.text = mensaje.contenido

            //Fecha
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            tvFecha.text = sdf.format(mensaje.fechaCreacion)

            tvEmisor.text = mensaje.emisor
        }
    }
}
