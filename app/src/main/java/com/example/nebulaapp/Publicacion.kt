package com.example.nebulaapp

import android.os.Parcel
import android.os.Parcelable

data class Publicacion(
    val id: Int,
    val usuarioId: Int,
    val texto: String,
    val imagen: ByteArray?,
    val nombreUsuario: String,
    val fechaCreacion: String
) : Parcelable {
    // Constructor utilizado para crear un objeto desde un Parcel
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.createByteArray(),
        parcel.readString() ?: "",
        parcel.readString() ?: ""
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
