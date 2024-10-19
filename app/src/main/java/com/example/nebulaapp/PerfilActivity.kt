package com.example.nebulaapp

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class PerfilActivity : AppCompatActivity() {

    private lateinit var ivProfileImage: ImageView
    private lateinit var tvUsername: TextView
    private lateinit var tvDescription: TextView // Nuevo TextView para la descripción
    private lateinit var btnEditProfile: Button
    private var username: String? = null // Nombre del usuario logueado

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        // Inicializar vistas
        ivProfileImage = findViewById(R.id.ivProfileImage)
        tvUsername = findViewById(R.id.tvUsername)
        tvDescription = findViewById(R.id.tvDescription) // Inicializar el TextView para la descripción
        btnEditProfile = findViewById(R.id.btnEditProfile)

        // Obtener el nombre del usuario desde el Intent
        username = intent.getStringExtra("USERNAME")

        // Verificar que el usuario no sea nulo
        username?.let {
            loadUserProfile(it) // Cargar los datos del perfil
        }

        btnEditProfile.setOnClickListener {
            val intent = Intent(this, ActualizarPerfilActivity::class.java)
            intent.putExtra("USERNAME", username) // Pasar el nombre de usuario
            startActivity(intent) // Navegar a la actividad de actualización
        }
    }

    private fun loadUserProfile(username: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val userData = getUserData(username)
            userData?.let {
                tvUsername.text = "@${it.username}" // Mostrar el nombre de usuario
                it.description?.let { description ->
                    tvDescription.text = description // Mostrar la descripción del usuario
                }
                it.photo?.let { photoBytes ->
                    ivProfileImage.setImageBitmap(byteArrayToBitmap(photoBytes)) // Cargar la imagen
                }
            }
        }
    }

    private suspend fun getUserData(username: String): UserData? {
        return withContext(Dispatchers.IO) {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"

            var connection: Connection? = null
            var preparedStatement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            try {
                connection = DriverManager.getConnection(url, user, pass)

                val query = "SELECT usuario, descripcion, photo FROM public.usuarios WHERE usuario = ?" // Incluye descripción en la consulta
                preparedStatement = connection.prepareStatement(query).apply {
                    setString(1, username)
                }

                resultSet = preparedStatement.executeQuery()

                return@withContext if (resultSet.next()) {
                    val photoBytes = resultSet.getBytes("photo")
                    val username = resultSet.getString("usuario") ?: "Usuario no disponible"
                    val description = resultSet.getString("descripcion") // Obtiene la descripción

                    UserData(username, description, photoBytes)
                } else {
                    null
                }

            } catch (e: SQLException) {
                e.printStackTrace()
                return@withContext null
            } finally {
                resultSet?.close()
                preparedStatement?.close()
                connection?.close()
            }
        }
    }

    private fun byteArrayToBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    data class UserData(val username: String, val description: String?, val photo: ByteArray?)
}
