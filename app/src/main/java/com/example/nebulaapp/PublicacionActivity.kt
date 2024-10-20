package com.example.nebulaapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException

class PublicacionActivity : AppCompatActivity() {

    private lateinit var etTexto: EditText
    private lateinit var btnSeleccionarImagen: Button
    private lateinit var btnPublicar: Button
    private lateinit var ivImagen: ImageView
    private lateinit var btnBack: Button

    private var username: String? = null
    private var imagenSeleccionada: ByteArray? = null

    companion object {
        private const val REQUEST_CODE_SELECT_IMAGE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publicacion)

        // Vincular vistas con sus IDs del XML
        etTexto = findViewById(R.id.etTexto)
        btnSeleccionarImagen = findViewById(R.id.btnSeleccionarImagen)
        btnPublicar = findViewById(R.id.btnPublicar)
        ivImagen = findViewById(R.id.ivImagen)
        btnBack = findViewById(R.id.btnRegresar)

        // Obtener el nombre del usuario del Intent
        username = intent.getStringExtra("USERNAME")

        // Configurar eventos de los botones
        btnSeleccionarImagen.setOnClickListener {
            abrirGaleria()
        }
        btnBack.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("USERNAME", username)
            startActivity(intent)
            finish()
        }

        btnPublicar.setOnClickListener {
            val texto = etTexto.text.toString().trim()

            if (texto.isEmpty()) {
                Toast.makeText(this, "Por favor, ingrese un texto.", Toast.LENGTH_SHORT).show()
            } else {
                username?.let {
                    realizarPublicacion(it, texto)
                } ?: run {
                    Toast.makeText(this, "Error: Usuario no encontrado.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun abrirGaleria() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            selectedImageUri?.let { uri ->
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                ivImagen.setImageBitmap(bitmap)
                imagenSeleccionada = bitmapToByteArray(bitmap)
            }
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray()
    }

    private fun realizarPublicacion(username: String, texto: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val publicacionExitosa = insertarPublicacionEnBaseDeDatos(username, texto, imagenSeleccionada)
            if (publicacionExitosa) {
                Toast.makeText(this@PublicacionActivity, "Publicación realizada con éxito.", Toast.LENGTH_SHORT).show()

                // Crear una nueva publicación para pasar de vuelta a HomeActivity
                val nuevaPublicacion = Publicacion(
                    id = 0,
                    usuarioId = 0,
                    texto = texto,
                    imagen = imagenSeleccionada,
                    nombreUsuario = username,
                    fechaCreacion = ""
                )

                // Preparar el resultado para enviar a HomeActivity
                val intent = Intent()
                intent.putExtra("NUEVA_PUBLICACION", nuevaPublicacion)
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this@PublicacionActivity, "Error al realizar la publicación.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun insertarPublicacionEnBaseDeDatos(
        username: String,
        texto: String,
        imagen: ByteArray?
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"

            var connection: Connection? = null
            var preparedStatement: PreparedStatement? = null

            try {
                connection = DriverManager.getConnection(url, user, pass)

                // Buscar el ID del usuario basado en su nombre
                val queryUsuario = "SELECT id FROM public.usuarios WHERE usuario = ?"
                preparedStatement = connection.prepareStatement(queryUsuario).apply {
                    setString(1, username)
                }
                val resultSet = preparedStatement.executeQuery()

                if (resultSet.next()) {
                    val usuarioId = resultSet.getInt("id")

                    // Insertar la publicación
                    val queryPublicacion = """
                        INSERT INTO public.publicaciones (usuario_id, texto, imagen, fecha_creacion)
                        VALUES (?, ?, ?, CURRENT_TIMESTAMP)
                    """
                    preparedStatement = connection.prepareStatement(queryPublicacion).apply {
                        setInt(1, usuarioId)
                        setString(2, texto)
                        setBytes(3, imagen)
                    }
                    preparedStatement.executeUpdate()
                    return@withContext true
                } else {
                    return@withContext false
                }
            } catch (e: SQLException) {
                e.printStackTrace()
                return@withContext false
            } finally {
                preparedStatement?.close()
                connection?.close()
            }
        }
    }
}
