package com.example.nebulaapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UserAdapter(
    private val users: List<Usuario>,
    private val onUserClick: (Usuario) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user, onUserClick)
    }

    override fun getItemCount() = users.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tvNombre)

        fun bind(user: Usuario, onUserClick: (Usuario) -> Unit) {
            // Mostrar el nombre y apellido del usuario
            tvNombre.text = StringBuilder().apply {
                append(user.nombre)
                append(" ")
                append(user.apellido)
            }.toString()


            itemView.setOnClickListener { onUserClick(user) }
        }
    }
}
