package com.example.nebulaapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PublicacionAdapter(
    private val context: Context,
    private var publicaciones: MutableList<Publicacion>
) : RecyclerView.Adapter<PublicacionAdapter.PublicacionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PublicacionViewHolder {
        // Inflar el layout para cada item de publicación
        val view = LayoutInflater.from(context).inflate(R.layout.item_publicacion, parent, false)
        return PublicacionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PublicacionViewHolder, position: Int) {
        val publicacion = publicaciones[position]
        holder.bind(publicacion)
    }

    override fun getItemCount(): Int {
        return publicaciones.size
    }

    inner class PublicacionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Inicializar los componentes de la vista
        private val tvNombreUsuario: TextView = itemView.findViewById(R.id.tvNombreUsuario)
        private val tvFechaCreacion: TextView = itemView.findViewById(R.id.tvFechaCreacion)
        private val tvTexto: TextView = itemView.findViewById(R.id.tvTexto)
        private val ivImagen: ImageView = itemView.findViewById(R.id.ivImagen)

        fun bind(publicacion: Publicacion) {
            // Asignar los datos a los componentes de la vista
            tvNombreUsuario.text = publicacion.nombreUsuario
            tvFechaCreacion.text = publicacion.fechaCreacion
            tvTexto.text = publicacion.texto

            // Verificar si hay imagen y establecer visibilidad
            if (publicacion.imagen != null && publicacion.imagen.isNotEmpty()) {
                ivImagen.setImageBitmap(byteArrayToBitmap(publicacion.imagen))
                ivImagen.visibility = View.VISIBLE
            } else {
                ivImagen.visibility = View.GONE
            }
        }

        private fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
            // Convertir el arreglo de bytes en un Bitmap
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        }
    }

    // Método para actualizar la lista de publicaciones
    fun updatePublicaciones(newPublicaciones: List<Publicacion>) {
        publicaciones.clear()
        publicaciones.addAll(newPublicaciones)
        notifyDataSetChanged()
    }

    // Método para agregar una nueva publicación
    fun agregarPublicacion(publicacion: Publicacion) {
        publicaciones.add(0, publicacion)
        notifyItemInserted(0)
    }
}
