package com.example.nebulaapp

import android.os.Parcel
import android.os.Parcelable

data class Publicacion(
    val id: Int,
    val usuarioId: Int,
    val texto: String,
    val imagen: ByteArray?,
    val nombreUsuario: String,
    val fechaCreacion: String,
    var likes: Int = 0
) : Parcelable {

    // Constructor utilizado para crear un objeto desde un Parcel
    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        usuarioId = parcel.readInt(),
        texto = parcel.readString() ?: "",
        imagen = parcel.createByteArray(),
        nombreUsuario = parcel.readString() ?: "",
        fechaCreacion = parcel.readString() ?: "",
        likes = parcel.readInt()
    )

    // Método que describe el contenido (normalmente se devuelve 0)
    override fun describeContents(): Int {
        return 0
    }

    // Método que escribe los datos en el Parcel
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(usuarioId)
        parcel.writeString(texto)
        parcel.writeByteArray(imagen)
        parcel.writeString(nombreUsuario)
        parcel.writeString(fechaCreacion)
        parcel.writeInt(likes)
    }

    // Método para incrementar el contador de likes
    fun incrementLikes() {
        likes++
    }

    // Creación del objeto Parcelable
    companion object CREATOR : Parcelable.Creator<Publicacion> {
        override fun createFromParcel(parcel: Parcel): Publicacion {
            return Publicacion(parcel)
        }

        override fun newArray(size: Int): Array<Publicacion?> {
            return arrayOfNulls(size)
        }
    }
}
