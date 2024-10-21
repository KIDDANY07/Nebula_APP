package com.example.nebulaapp

import android.content.Intent
import android.os.Bundle
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

class UserListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var userAdapter: UserAdapter
    private var username: String? = null
    private val usersList = mutableListOf<Usuario>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_list)

        // Obtener el nombre de usuario del intent
        username = intent.getStringExtra("USERNAME")

        recyclerView = findViewById(R.id.recyclerView)
        userAdapter = UserAdapter(usersList) { selectedUser -> onUserSelected(selectedUser) }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = userAdapter

        loadUsers()
    }

    private fun loadUsers() {
        CoroutineScope(Dispatchers.Main).launch {
            val users = fetchUsers()
            usersList.clear()
            usersList.addAll(users)
            userAdapter.notifyDataSetChanged()
        }
    }

    private suspend fun fetchUsers(): List<Usuario> {
        return withContext(Dispatchers.IO) {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"

            var connection: Connection? = null
            var preparedStatement: PreparedStatement? = null
            val users = mutableListOf<Usuario>()

            try {
                connection = DriverManager.getConnection(url, user, pass)
                val query = "SELECT id, nombre, apellido, usuario FROM public.usuarios"
                preparedStatement = connection.prepareStatement(query)
                val resultSet: ResultSet = preparedStatement.executeQuery()

                while (resultSet.next()) {
                    val id = resultSet.getInt("id")
                    val nombre = resultSet.getString("nombre")
                    val apellido = resultSet.getString("apellido")
                    val usuario = resultSet.getString("usuario")

                    // Solo añadir usuarios que no sean el que ha iniciado sesión
                    if (usuario != username) {
                        users.add(Usuario(id, nombre, apellido, usuario))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                preparedStatement?.close()
                connection?.close()
            }

            return@withContext users
        }
    }

    private fun onUserSelected(selectedUser: Usuario) {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra("USERNAME", username)
        intent.putExtra("SELECTED_USER_ID", selectedUser.id)
        intent.putExtra("SELECTED_USER_USERNAME", selectedUser.usuario)
        startActivity(intent)
    }
}
