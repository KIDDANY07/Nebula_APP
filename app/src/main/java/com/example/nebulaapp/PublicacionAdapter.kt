package com.example.nebulaapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PublicacionAdapter(
    private val context: Context,
    private var publicaciones: MutableList<Publicacion>,
    private val onLikeClicked: (Publicacion) -> Unit
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
        private val btnLike: Button = itemView.findViewById(R.id.btnLike)
        private val tvLikesCount: TextView = itemView.findViewById(R.id.tvLikesCount)

        fun bind(publicacion: Publicacion) {
            // Asignar los datos a los componentes de la vista
            tvNombreUsuario.text = publicacion.nombreUsuario
            tvFechaCreacion.text = publicacion.fechaCreacion
            tvTexto.text = publicacion.texto
            tvLikesCount.text = "Likes: ${publicacion.likes}"

            // Verificar si hay imagen y establecer visibilidad
            if (publicacion.imagen != null) {
                ivImagen.setImageBitmap(byteArrayToBitmap(publicacion.imagen))
                ivImagen.visibility = View.VISIBLE
            } else {
                ivImagen.visibility = View.GONE
            }

            // Configurar el botón de Like
            btnLike.setOnClickListener {
                // Deshabilitar el botón para evitar múltiples clics
                btnLike.isEnabled = false
                onLikeClicked(publicacion)
            }
        }

        private fun byteArrayToBitmap(byteArray: ByteArray?): Bitmap? {
            // Convertir el arreglo de bytes en un Bitmap
            return byteArray?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
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

    // Método para habilitar nuevamente el botón de like para una publicación
    fun enableLikeButton(publicacion: Publicacion) {
        val position = publicaciones.indexOf(publicacion)
        if (position >= 0) {
            notifyItemChanged(position)
        }
    }
}
