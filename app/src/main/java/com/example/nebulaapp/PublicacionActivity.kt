package com.example.nebulaapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException

class PublicacionActivity : AppCompatActivity() {

    private lateinit var etTexto: EditText
    private lateinit var btnSeleccionarImagen: Button
    private lateinit var btnPublicar: Button
    private lateinit var ivImagen: ImageView
    private var selectedImageUri: Uri? = null

    companion object {
        const val IMAGE_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publicacion)

        etTexto = findViewById(R.id.etTexto)
        btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagen)
        btnPublicar = findViewById(R.id.btnPublicar)
        ivImagen = findViewById(R.id.ivImagen)

        btnSeleccionarImagen.setOnClickListener {
            // Abrir la galería para seleccionar una imagen
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, IMAGE_REQUEST_CODE)
        }

        btnPublicar.setOnClickListener {
            val texto = etTexto.text.toString()
            if (texto.isEmpty() || selectedImageUri == null) {
                Toast.makeText(this, "Por favor, completa el texto y selecciona una imagen.", Toast.LENGTH_SHORT).show()
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    val success = publicar(texto, selectedImageUri!!)
                    if (success) {
                        Toast.makeText(this@PublicacionActivity, "Publicación exitosa.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@PublicacionActivity, "Error al publicar. Intente nuevamente.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            ivImagen.setImageURI(selectedImageUri)
        }
    }

    private suspend fun publicar(texto: String, imageUri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"

            var connection: Connection? = null
            var preparedStatement: PreparedStatement? = null

            try {
                connection = DriverManager.getConnection(url, user, pass)

                // Convertir la imagen seleccionada a byte array
                val imageBytes = imageUriToByteArray(imageUri)

                // Consulta para insertar la publicación
                val query = "INSERT INTO public.publicaciones (usuario_id, texto, imagen) VALUES (?, ?, ?)"
                preparedStatement = connection.prepareStatement(query).apply {
                    setInt(1, 1) // Cambia 1 por el ID del usuario logueado
                    setString(2, texto)
                    setBytes(3, imageBytes)
                }

                preparedStatement.executeUpdate()
                return@withContext true

            } catch (e: SQLException) {
                e.printStackTrace()
                return@withContext false
            } finally {
                preparedStatement?.close()
                connection?.close()
            }
        }
    }

    private fun imageUriToByteArray(imageUri: Uri): ByteArray? {
        val inputStream = contentResolver.openInputStream(imageUri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream) // Puedes ajustar el formato y la calidad
        return stream.toByteArray()
    }
}
