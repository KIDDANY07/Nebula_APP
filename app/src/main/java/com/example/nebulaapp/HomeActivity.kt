package com.example.nebulaapp
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.nebulaapp.Publicacion
import com.example.nebulaapp.PublicacionActivity
import com.example.nebulaapp.PublicacionAdapter
import com.example.nebulaapp.R
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        publicacionAdapter = PublicacionAdapter(this, emptyList())
        recyclerView.adapter = publicacionAdapter

        loadPublicaciones()

        findViewById<View>(R.id.btnPublicar).setOnClickListener {
            val intent = Intent(this, PublicacionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadPublicaciones() {
        CoroutineScope(Dispatchers.Main).launch {
            val publicaciones = getPublicacionesDesdeBaseDeDatos()
            publicacionAdapter.updatePublicaciones(publicaciones)
        }
    }

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
