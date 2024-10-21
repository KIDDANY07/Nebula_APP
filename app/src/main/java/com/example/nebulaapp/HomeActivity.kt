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
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

class HomeActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var publicacionAdapter: PublicacionAdapter

    // Variable para guardar el nombre de usuario recibido
    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Recibir el nombre de usuario desde LoginActivity
        username = intent.getStringExtra("USERNAME")

        // Verificar si el nombre de usuario es nulo
        if (username.isNullOrEmpty()) {
            Toast.makeText(this, "Error: Usuario no encontrado.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Inicializar RecyclerView y su Adapter
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        publicacionAdapter = PublicacionAdapter(this, mutableListOf())
        recyclerView.adapter = publicacionAdapter

        // Cargar publicaciones desde la base de datos
        loadPublicaciones()

        // Configurar los botones para publicar y ver el perfil
        findViewById<View>(R.id.btnPublicar).setOnClickListener {
            val intent = Intent(this, PublicacionActivity::class.java)
            intent.putExtra("USERNAME", username)
            startActivityForResult(intent, REQUEST_CODE_PUBLICAR)
        }

        findViewById<View>(R.id.btnPerfil).setOnClickListener {
            val intent = Intent(this, PerfilActivity::class.java)
            intent.putExtra("USERNAME", username)
            startActivity(intent)
        }

        // Agregar la funcionalidad para el botón "Chat"
        val button = findViewById<Button>(R.id.btnChat)
        button.setOnClickListener {
            val intent = Intent(this, UserListActivity::class.java)
            intent.putExtra("USERNAME", username)
            startActivity(intent)
        }
    }

    // Código de solicitud para la actividad de publicación
    private val REQUEST_CODE_PUBLICAR = 1

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PUBLICAR && resultCode == RESULT_OK) {

            val nuevaPublicacion = data?.getParcelableExtra<Publicacion>("NUEVA_PUBLICACION")
            if (nuevaPublicacion != null) {
                publicacionAdapter.agregarPublicacion(nuevaPublicacion)
            }
        }
    }

    // Función para cargar las publicaciones en la interfaz
    private fun loadPublicaciones() {
        CoroutineScope(Dispatchers.Main).launch {
            val publicaciones = getPublicacionesDesdeBaseDeDatos()
            publicacionAdapter.updatePublicaciones(publicaciones)
        }
    }

    // Suspensión para obtener publicaciones desde la base de datos
    private suspend fun getPublicacionesDesdeBaseDeDatos(): List<Publicacion> {
        return withContext(Dispatchers.IO) {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"
            val publicaciones = mutableListOf<Publicacion>()

            var connection: Connection? = null
            var preparedStatement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            try {
                connection = DriverManager.getConnection(url, user, pass)
                val query = """
                    SELECT p.id, p.usuario_id, p.texto, p.imagen, p.fecha_creacion, u.usuario AS nombre_usuario
                    FROM public.publicaciones p
                    JOIN public.usuarios u ON p.usuario_id = u.id
                    ORDER BY p.fecha_creacion DESC
                """
                preparedStatement = connection.prepareStatement(query)
                resultSet = preparedStatement.executeQuery()

                while (resultSet.next()) {
                    val id = resultSet.getInt("id")
                    val usuarioId = resultSet.getInt("usuario_id")
                    val texto = resultSet.getString("texto")
                    val imagen = resultSet.getBytes("imagen")
                    val nombreUsuario = resultSet.getString("nombre_usuario")
                    val fechaCreacion = resultSet.getTimestamp("fecha_creacion")?.toString() ?: ""

                    publicaciones.add(Publicacion(id, usuarioId, texto, imagen, nombreUsuario, fechaCreacion))
                }
            } catch (e: SQLException) {
                e.printStackTrace()
            } finally {
                resultSet?.close()
                preparedStatement?.close()
                connection?.close()
            }

            publicaciones
        }
    }
}
