package com.example.nebulaapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt
import java.sql.*

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnToReg: Button

    // Definimos constantes para la conexión a la base de datos
    companion object {
        private const val DB_URL = "jdbc:postgresql://10.0.2.2:5432/Nebula"
        private const val DB_USER = "postgres"
        private const val DB_PASSWORD = "password"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializamos los elementos de la vista
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnToReg = findViewById(R.id.btnToReg)

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                showToast("Por favor, complete todos los campos.")
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    val (loginSuccess, userId) = loginUser(username, password)
                    if (loginSuccess) {
                        showToast("Inicio de sesión exitoso.")
                        // Redirigimos a HomeActivity y pasamos la ID del usuario
                        val intent = Intent(this@LoginActivity, HomeActivity::class.java).apply {
                            putExtra("USERNAME", username)
                            putExtra("USER_ID", userId)
                        }
                        startActivity(intent)
                        finish()
                    } else {
                        showToast("Credenciales incorrectas. Intente de nuevo.")
                    }
                }
            }
        }

        btnToReg.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    // Función para mostrar Toasts en el hilo principal
    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Función suspendida para validar el usuario y la contraseña
    private suspend fun loginUser(username: String, password: String): Pair<Boolean, Int?> {
        return withContext(Dispatchers.IO) {
            var connection: Connection? = null
            var preparedStatement: PreparedStatement? = null
            var resultSet: ResultSet? = null

            try {
                // Conectamos con la base de datos
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)

                val query = "SELECT contrasena, id FROM public.usuarios WHERE usuario = ?"
                preparedStatement = connection.prepareStatement(query).apply {
                    setString(1, username)
                }

                resultSet = preparedStatement.executeQuery()

                return@withContext if (resultSet.next()) {
                    val hashedPassword = resultSet.getString("contrasena")
                    val userId = resultSet.getInt("id")
                    if (BCrypt.checkpw(password, hashedPassword)) {
                        Pair(true, userId)
                    } else {
                        Pair(false, null)
                    }
                } else {
                    showToast("Usuario no encontrado.")
                    Pair(false, null)
                }

            } catch (e: SQLException) {
                e.printStackTrace()
                showToast("Error de conexión con la base de datos.")
                Pair(false, null)
            } finally {
                // Cerramos los recursos en orden inverso de apertura
                try {
                    resultSet?.close()
                    preparedStatement?.close()
                    connection?.close()
                } catch (e: SQLException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
