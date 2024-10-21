package com.example.nebulaapp

data class Usuario(
    val id: Int,
    val nombre: String,
    val apellido: String,
    val usuario: String
) {
    init {
        require(id >= 0) { "El ID no puede ser negativo." }
        require(nombre.isNotBlank()) { "El nombre no puede estar vacío." }
        require(apellido.isNotBlank()) { "El apellido no puede estar vacío." }
        require(usuario.isNotBlank()) { "El nombre de usuario no puede estar vacío." }
    }

    override fun toString(): String {
        return "$nombre $apellido ($usuario)"
    }
}
