package com.example.nebulaapp

import android.content.Intent
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

class HomeActivity : AppCompatActivity() {

    private lateinit var btnPost: Button
    private lateinit var etPostText: EditText
    private lateinit var ivPostImage: ImageView
    private var selectedImageUri: Uri? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val username = intent.getStringExtra("username") ?: return

        btnPost = findViewById(R.id.btnPost)
        etPostText = findViewById(R.id.etPostText)
        ivPostImage = findViewById(R.id.ivPostImage)

        // Abrir selector de imágenes
        ivPostImage.setOnClickListener {
            openImagePicker()
        }

        // Crear publicación
        btnPost.setOnClickListener {
            val postText = etPostText.text.toString()
            if (postText.isEmpty() && selectedImageUri == null) {
                Toast.makeText(this, "Ingrese texto o seleccione una imagen.", Toast.LENGTH_SHORT).show()
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    val success = postPublication(postText, selectedImageUri, username)
                    if (success) {
                        Toast.makeText(this@HomeActivity, "Publicación realizada con éxito.", Toast.LENGTH_SHORT).show()
                        etPostText.text.clear()
                        ivPostImage.setImageResource(0)
                        selectedImageUri = null
                    } else {
                        Toast.makeText(this@HomeActivity, "Error al realizar la publicación.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Abrir selector de imágenes
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            ivPostImage.setImageURI(selectedImageUri)
        }
    }

    // Función para realizar la publicación
    private suspend fun postPublication(text: String, imageUri: Uri?, username: String): Boolean {
        return withContext(Dispatchers.IO) {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"

            var connection: Connection? = null
            var preparedStatement: PreparedStatement? = null

            try {
                // Conectar a la base de datos
                connection = DriverManager.getConnection(url, user, pass)

                // Obtener el ID del usuario
                val userId = getUserId(connection, username) ?: throw SQLException("Usuario no encontrado")

                // Convertir la imagen a bytearray si existe
                val imageBytes = imageUri?.let { uri ->
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        ByteArrayOutputStream().use { byteArrayOutputStream ->
                            inputStream.copyTo(byteArrayOutputStream)
                            byteArrayOutputStream.toByteArray()
                        }
                    }
                }

                // Insertar la publicación en la base de datos
                val query = "INSERT INTO public.publicaciones (usuario_id, texto, imagen) VALUES (?, ?, ?)"
                preparedStatement = connection.prepareStatement(query).apply {
                    setInt(1, userId)
                    setString(2, text)
                    setBytes(3, imageBytes) // Puede ser null
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

    // Obtener el ID del usuario
    private fun getUserId(connection: Connection, username: String): Int? {
        val query = "SELECT id FROM public.usuarios WHERE usuario = ?"
        connection.prepareStatement(query).use { statement ->
            statement.setString(1, username)
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) resultSet.getInt("id") else null
            }
        }
    }
}
