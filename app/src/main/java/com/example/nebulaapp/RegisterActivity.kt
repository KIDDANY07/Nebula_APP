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
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etApellido = findViewById<EditText>(R.id.etApellido)
        val etCorreo = findViewById<EditText>(R.id.etCorreo)
        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val btnToLogin = findViewById<Button>(R.id.btnToLogin)

        btnRegister.setOnClickListener {
            val nombre = etNombre.text.toString()
            val apellido = etApellido.text.toString()
            val correo = etCorreo.text.toString()
            val usuario = etUsuario.text.toString()
            val password = etPassword.text.toString()

            // Verificar que todos los campos estén completos
            if (nombre.isEmpty() || apellido.isEmpty() || correo.isEmpty() || usuario.isEmpty() || password.isEmpty()) {
                Toast.makeText(this@RegisterActivity, "Por favor, complete todos los campos.", Toast.LENGTH_SHORT).show()
            } else {
                // Registrar al usuario
                CoroutineScope(Dispatchers.Main).launch {
                    val registrationSuccess = registerUser(nombre, apellido, correo, usuario, password)
                    if (registrationSuccess) {
                        Toast.makeText(this@RegisterActivity, "Registro exitoso.", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, "Error en el registro. Intente de nuevo.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnToLogin.setOnClickListener{
            val intent = Intent(this,LoginActivity::class.java)
            startActivity(intent)
        }
    }

    // Función para registrar un usuario en la base de datos
    private suspend fun registerUser(nombre: String, apellido: String, correo: String, usuario: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            val url = "jdbc:postgresql://10.0.2.2:5432/Nebula"
            val user = "postgres"
            val pass = "password"

            var connection: Connection? = null
            var preparedStatement: PreparedStatement? = null

            try {
                connection = DriverManager.getConnection(url, user, pass)

                // Hashear la contraseña antes de guardarla
                val hashedPassword = hashPassword(password)

                // Consulta para insertar el nuevo usuario
                val query = "INSERT INTO public.usuarios (nombre, apellido, correo, contrasena, usuario) " +
                        "VALUES (?, ?, ?, ?, ?)"

                preparedStatement = connection.prepareStatement(query).apply {
                    setString(1, nombre)
                    setString(2, apellido)
                    setString(3, correo)
                    setString(4, hashedPassword)
                    setString(5, usuario)
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

    // Función para hashear la contraseña utilizando bcrypt
    private fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
}
