package com.example.nebulaapp

import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

data class Mensaje(
    val id: Int,
    val contenido: String,
    val fechaCreacion: Date,
    val emisor: String,
    val receptor: String
) {

    fun getFormattedFecha(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(fechaCreacion)
    }
}
