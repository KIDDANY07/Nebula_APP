package com.example.nebulaapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.sql.DriverManager
import java.sql.SQLException

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var publicacionAdapter: PublicacionAdapter
    private var username: String? = null
    private var userId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Recibir el nombre de usuario y el ID desde LoginActivity
        username = intent.getStringExtra("USERNAME") ?: run {
            showToast("Error: Usuario no encontrado.")
            finish()
            return
        }
        userId = intent.getIntExtra("USER_ID", -1)

        if (userId == -1) {
            showToast("Error: ID de usuario no encontrado.")
            finish()
            return
        }

        // Inicializar RecyclerView y su Adapter
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        publicacionAdapter = PublicacionAdapter(this, mutableListOf()) { publicacion ->
            handleLike(publicacion)
        }
        recyclerView.adapter = publicacionAdapter

        // Cargar publicaciones desde la base de datos
        loadPublicaciones()

        // Configurar botones
        findViewById<View>(R.id.btnPublicar).setOnClickListener {
            startActivityForResult(Intent(this, PublicacionActivity::class.java).apply {
                putExtra("USERNAME", username)
            }, REQUEST_CODE_PUBLICAR)
        }

        findViewById<View>(R.id.btnPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java).apply {
                putExtra("USERNAME", username)
            })
        }

        findViewById<Button>(R.id.btnChat).setOnClickListener {
            startActivity(Intent(this, UserListActivity::class.java).apply {
                putExtra("USERNAME", username)
            })
        }
    }

    private val REQUEST_CODE_PUBLICAR = 1

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PUBLICAR && resultCode == RESULT_OK) {
            data?.getParcelableExtra<Publicacion>("NUEVA_PUBLICACION")?.let { nuevaPublicacion ->
                publicacionAdapter.agregarPublicacion(nuevaPublicacion)
            }
        }
    }

    private fun loadPublicaciones() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val publicaciones = getPublicacionesDesdeBaseDeDatos()
                publicacionAdapter.updatePublicaciones(publicaciones)
            } catch (e: Exception) {
                showToast("Error al cargar publicaciones: ${e.message}")
            }
        }
    }

    private suspend fun getPublicacionesDesdeBaseDeDatos(): List<Publicacion> {
        return withContext(Dispatchers.IO) {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"
            val publicaciones = mutableListOf<Publicacion>()

            try {
                DriverManager.getConnection(url, user, pass).use { connection ->
                    val query = """
                        SELECT p.id, p.usuario_id, p.texto, p.imagen, p.fecha_creacion, 
                               u.usuario AS nombre_usuario, p.likes
                        FROM public.publicaciones p
                        JOIN public.usuarios u ON p.usuario_id = u.id
                        ORDER BY p.fecha_creacion DESC
                    """
                    connection.prepareStatement(query).use { preparedStatement ->
                        preparedStatement.executeQuery().use { resultSet ->
                            while (resultSet.next()) {
                                val id = resultSet.getInt("id")
                                val usuarioId = resultSet.getInt("usuario_id")
                                val texto = resultSet.getString("texto")
                                val imagen = resultSet.getBytes("imagen")
                                val nombreUsuario = resultSet.getString("nombre_usuario")
                                val fechaCreacion = resultSet.getTimestamp("fecha_creacion")?.toString() ?: ""
                                val likes = resultSet.getInt("likes")

                                publicaciones.add(Publicacion(id, usuarioId, texto, imagen, nombreUsuario, fechaCreacion, likes))
                            }
                        }
                    }
                }
            } catch (e: SQLException) {
                e.printStackTrace()
                throw e
            }

            publicaciones
        }
    }

    private fun handleLike(publicacion: Publicacion) {
        CoroutineScope(Dispatchers.Main).launch {
            // Verificar si el usuario ya ha dado like a la publicación
            if (haDadoLike(publicacion)) {
                showToast("Ya has dado like a esta publicación")
                return@launch
            }

            // Incrementar contador de likes y registrar el like en la base de datos
            publicacion.incrementLikes()
            registrarLikeEnBaseDeDatos(publicacion)
            publicacionAdapter.notifyDataSetChanged()
            showToast("Te gusta esta publicación: ${publicacion.likes} likes")
        }
    }

    private suspend fun haDadoLike(publicacion: Publicacion): Boolean {
        return withContext(Dispatchers.IO) {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"
            var liked = false // Inicializa la variable

            try {
                DriverManager.getConnection(url, user, pass).use { connection ->
                    val query = "SELECT COUNT(*) FROM likes WHERE usuario_id = ? AND publicacion_id = ?"
                    connection.prepareStatement(query).use { preparedStatement ->
                        preparedStatement.setInt(1, userId ?: -1)
                        preparedStatement.setInt(2, publicacion.id)
                        preparedStatement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                liked = resultSet.getInt(1) > 0
                            }
                        }
                    }
                }
            } catch (e: SQLException) {
                e.printStackTrace()
                false // Devuelve false en caso de error
            }
            liked
        }
    }

    private fun registrarLikeEnBaseDeDatos(publicacion: Publicacion) {
        CoroutineScope(Dispatchers.IO).launch {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"

            try {
                DriverManager.getConnection(url, user, pass).use { connection ->
                    val query = "INSERT INTO likes (usuario_id, publicacion_id) VALUES (?, ?)"
                    connection.prepareStatement(query).use { preparedStatement ->
                        preparedStatement.setInt(1, userId ?: -1)
                        preparedStatement.setInt(2, publicacion.id)
                        preparedStatement.executeUpdate()
                    }
                    actualizarContadorLikes(publicacion)
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    private fun actualizarContadorLikes(publicacion: Publicacion) {
        CoroutineScope(Dispatchers.IO).launch {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"

            try {
                DriverManager.getConnection(url, user, pass).use { connection ->
                    val query = "UPDATE public.publicaciones SET likes = ? WHERE id = ?"
                    connection.prepareStatement(query).use { preparedStatement ->
                        preparedStatement.setInt(1, publicacion.likes)
                        preparedStatement.setInt(2, publicacion.id)
                        preparedStatement.executeUpdate()
                    }
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
