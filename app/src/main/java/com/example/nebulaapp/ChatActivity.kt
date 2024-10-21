package com.example.nebulaapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView // Importar TextView
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

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var etMensaje: EditText
    private lateinit var btnEnviar: Button
    private lateinit var tvChatWith: TextView
    private var mensajesList = mutableListOf<Mensaje>()
    private var username: String? = null
    private var selectedUserId: Int? = null
    private var selectedUserUsername: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Obtener datos de la intención
        username = intent.getStringExtra("USERNAME")
        selectedUserId = intent.getIntExtra("SELECTED_USER_ID", -1)
        selectedUserUsername = intent.getStringExtra("SELECTED_USER_USERNAME")

        // Configurar RecyclerView y Adapter
        recyclerView = findViewById(R.id.recyclerView)
        etMensaje = findViewById(R.id.etMensaje)
        btnEnviar = findViewById(R.id.btnEnviar)
        tvChatWith = findViewById(R.id.tvChatWith) // Inicializar el TextView

        chatAdapter = ChatAdapter(mensajesList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter

        // Establecer el texto del TextView para mostrar con quién estás chateando
        tvChatWith.text = "Chateando con: $selectedUserUsername"

        loadMensajes()

        //Boton enviar
        btnEnviar.setOnClickListener {
            val contenido = etMensaje.text.toString().trim()
            if (contenido.isNotEmpty()) {
                enviarMensaje(contenido)
                etMensaje.text.clear()
            }
        }
    }

    private fun loadMensajes() {
        CoroutineScope(Dispatchers.Main).launch {
            val mensajes = fetchMensajes()
            mensajesList.clear()
            mensajesList.addAll(mensajes)
            chatAdapter.notifyDataSetChanged()
        }
    }

    private suspend fun fetchMensajes(): List<Mensaje> {
        return withContext(Dispatchers.IO) {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"

            val mensajes = mutableListOf<Mensaje>()

            var connection: Connection? = null
            var preparedStatement: PreparedStatement? = null

            try {
                connection = DriverManager.getConnection(url, user, pass)
                val query = """
                    SELECT m.id, m.contenido, m.fecha_creacion, u1.usuario AS emisor
                    FROM public.mensajes m
                    JOIN public.usuarios u1 ON m.emisor_id = u1.id
                    WHERE (m.emisor_id = ? AND m.receptor_id = ?) OR (m.emisor_id = ? AND m.receptor_id = ?)
                    ORDER BY m.fecha_creacion
                """
                preparedStatement = connection.prepareStatement(query)
                preparedStatement.setInt(1, selectedUserId ?: -1)
                preparedStatement.setInt(2, getUserIdByUsername(username) ?: -1)
                preparedStatement.setInt(3, getUserIdByUsername(username) ?: -1)
                preparedStatement.setInt(4, selectedUserId ?: -1)

                val resultSet: ResultSet = preparedStatement.executeQuery()

                while (resultSet.next()) {
                    val id = resultSet.getInt("id")
                    val contenido = resultSet.getString("contenido")
                    val fechaCreacion = resultSet.getTimestamp("fecha_creacion")
                    val emisor = resultSet.getString("emisor") // Solo el emisor
                    mensajes.add(Mensaje(id, contenido, fechaCreacion, emisor, ""))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                preparedStatement?.close()
                connection?.close()
            }

            return@withContext mensajes
        }
    }

    private fun enviarMensaje(contenido: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val success = sendMessage(contenido)
            if (success) {
                loadMensajes()
            }
        }
    }

    private suspend fun sendMessage(contenido: String): Boolean {
        return withContext(Dispatchers.IO) {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"

            var connection: Connection? = null
            var preparedStatement: PreparedStatement? = null

            try {
                connection = DriverManager.getConnection(url, user, pass)
                val query = "INSERT INTO public.mensajes (emisor_id, receptor_id, contenido) VALUES (?, ?, ?)"
                preparedStatement = connection.prepareStatement(query)
                preparedStatement.setInt(1, getUserIdByUsername(username) ?: -1)
                preparedStatement.setInt(2, selectedUserId ?: -1)
                preparedStatement.setString(3, contenido)
                preparedStatement.executeUpdate()
                return@withContext true
            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext false
            } finally {
                preparedStatement?.close()
                connection?.close()
            }
        }
    }

    private suspend fun getUserIdByUsername(username: String?): Int? {
        return withContext(Dispatchers.IO) {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"
            var connection: Connection? = null
            var preparedStatement: PreparedStatement? = null
            var userId: Int? = null

            try {
                connection = DriverManager.getConnection(url, user, pass)
                val query = "SELECT id FROM public.usuarios WHERE usuario = ?"
                preparedStatement = connection.prepareStatement(query)
                preparedStatement.setString(1, username)
                val resultSet: ResultSet = preparedStatement.executeQuery()

                if (resultSet.next()) {
                    userId = resultSet.getInt("id")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                preparedStatement?.close()
                connection?.close()
            }
            return@withContext userId
        }
    }
}
