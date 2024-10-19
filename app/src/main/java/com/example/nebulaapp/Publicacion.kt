package com.example.nebulaapp

data class Publicacion(
    val id: Int,
    val usuarioId: Int,
    val texto: String,
    val imagen: ByteArray?, // Esta propiedad debe ser un ByteArray o null
    val nombreUsuario: String, // Nombre del usuario
    val fechaCreacion: String // Fecha de creación en formato String
)
