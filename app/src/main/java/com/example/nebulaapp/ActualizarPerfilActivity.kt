package com.example.nebulaapp

import android.content.Intent
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
import java.sql.ResultSet
import java.sql.SQLException

class ActualizarPerfilActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etUsuario: EditText
    private lateinit var etDescripcion: EditText // Nueva EditText para la descripción
    private lateinit var ivProfileImage: ImageView
    private lateinit var btnUpdate: Button
    private var selectedImageUri: Uri? = null
    private var username: String? = null // Nombre de usuario recibido de la actividad anterior

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actualizar_perfil)

        // Inicializar las vistas
        etNombre = findViewById(R.id.etNombre)
        etApellido = findViewById(R.id.etApellido)
        etCorreo = findViewById(R.id.etCorreo)
        etUsuario = findViewById(R.id.etUsuario)
        etDescripcion = findViewById(R.id.etDescripcion) // Inicialización de la descripción
        ivProfileImage = findViewById(R.id.ivProfileImage)
        btnUpdate = findViewById(R.id.btnSave)

        // Obtener el nombre de usuario pasado desde PerfilActivity
        username = intent.getStringExtra("USERNAME") // Cambiado a la misma clave

        // Verifica que el username no sea nulo o vacío
        if (username.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No se recibió el nombre de usuario.", Toast.LENGTH_SHORT).show()
            finish() // Cierra la actividad si no hay nombre de usuario
            return
        }

        // Cargar información del usuario si se recibió el nombre
        CoroutineScope(Dispatchers.Main).launch {
            loadUserData(username!!)
        }

        ivProfileImage.setOnClickListener {
            openImagePicker()
        }

        btnUpdate.setOnClickListener {
            val nombre = etNombre.text.toString()
            val apellido = etApellido.text.toString()
            val correo = etCorreo.text.toString()
            val usuario = etUsuario.text.toString()
            val descripcion = etDescripcion.text.toString() // Obtener la descripción

            if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty() || usuario.isEmpty() || descripcion.isEmpty()) {
                Toast.makeText(this, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    val updateSuccess = updateUserProfile(nombre, apellido, correo, usuario, descripcion, selectedImageUri)
                    if (updateSuccess) {
                        Toast.makeText(this@ActualizarPerfilActivity, "Perfil actualizado con éxito.", Toast.LENGTH_SHORT).show()
                        // Volver a PerfilActivity pasando el usuario actualizado
                        val intent = Intent(this@ActualizarPerfilActivity, PerfilActivity::class.java)
                        intent.putExtra("USERNAME", usuario) // Pasar el usuario actualizado
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@ActualizarPerfilActivity, "Error al actualizar el perfil. Intente de nuevo.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data
            ivProfileImage.setImageURI(selectedImageUri) // Mostrar la imagen seleccionada
        }
    }

    private suspend fun updateUserProfile(
        nombre: String, apellido: String, correo: String, usuario: String, descripcion: String, imageUri: Uri?
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"

            var connection: Connection? = null
            var preparedStatement: PreparedStatement? = null

            try {
                connection = DriverManager.getConnection(url, user, pass)

                // Convertir la imagen a bytes si se seleccionó una
                val imageBytes = imageUri?.let { uri ->
                    contentResolver.openInputStream(uri)?.use { inputStream ->
                        ByteArrayOutputStream().use { byteArrayOutputStream ->
                            inputStream.copyTo(byteArrayOutputStream)
                            byteArrayOutputStream.toByteArray()
                        }
                    }
                }

                // Consulta para actualizar el perfil del usuario
                val query = """
                    UPDATE public.usuarios 
                    SET nombre = ?, apellido = ?, correo = ?, usuario = ?, descripcion = ?, photo = ? 
                    WHERE usuario = ?
                """.trimIndent()

                preparedStatement = connection.prepareStatement(query).apply {
                    setString(1, nombre)
                    setString(2, apellido)
                    setString(3, correo)
                    setString(4, usuario)
                    setString(5, descripcion) // Agregar descripción
                    setBytes(6, imageBytes) // Manejo de imagen
                    setString(7, username) // Usar el nombre de usuario original para actualizar
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

    private suspend fun loadUserData(username: String) {
        withContext(Dispatchers.IO) {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"

            var connection: Connection? = null
            var preparedStatement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            try {
                connection = DriverManager.getConnection(url, user, pass)

                // Consulta para obtener los datos del usuario
                val query = """
                    SELECT nombre, apellido, correo, usuario, descripcion, photo 
                    FROM public.usuarios 
                    WHERE usuario = ?
                """.trimIndent()

                preparedStatement = connection.prepareStatement(query).apply {
                    setString(1, username) // Usar el nombre de usuario como parámetro
                }

                resultSet = preparedStatement.executeQuery()

                if (resultSet.next()) {
                    val nombre = resultSet.getString("nombre")
                    val apellido = resultSet.getString("apellido")
                    val correo = resultSet.getString("correo")
                    val usuario = resultSet.getString("usuario")
                    val descripcion = resultSet.getString("descripcion") // Cargar descripción
                    val photo = resultSet.getBytes("photo")

                    // Actualizar la UI en el hilo principal
                    runOnUiThread {
                        etNombre.setText(nombre)
                        etApellido.setText(apellido)
                        etCorreo.setText(correo)
                        etUsuario.setText(usuario)
                        etDescripcion.setText(descripcion) // Establecer la descripción
                        if (photo != null) {
                            val bmp = BitmapFactory.decodeByteArray(photo, 0, photo.size)
                            ivProfileImage.setImageBitmap(bmp)
                        }
                    }
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            } finally {
                resultSet?.close()
                preparedStatement?.close()
                connection?.close()
            }
        }
    }
}
