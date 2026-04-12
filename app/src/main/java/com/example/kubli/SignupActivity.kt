package com.example.kubli

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.security.MessageDigest

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Link to Sign In
        val textSignIn = findViewById<TextView>(R.id.textSignIn)
        textSignIn.setOnClickListener {
            val intent = Intent(this, SigninActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Create Account Button
        val btnCreate = findViewById<Button>(R.id.btnCreateAccount)

        btnCreate.setOnClickListener {
            val nameLayout = findViewById<TextInputLayout>(R.id.inputName)
            val emailLayout = findViewById<TextInputLayout>(R.id.inputEmail)
            val passLayout = findViewById<TextInputLayout>(R.id.inputPassword)

            val name = nameLayout.editText?.text.toString().trim()
            val email = emailLayout.editText?.text.toString().trim()
            val password = passLayout.editText?.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Database Operations
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val existingUser = db.userDao().getUserByEmail(email)

                if (existingUser != null) {
                    Toast.makeText(this@SignupActivity, "Email already exists!", Toast.LENGTH_SHORT).show()
                } else {
                    // Hash Password & Insert User
                    val securePassword = hashPassword(password)
                    val newUser = User(fullName = name, email = email, passwordHash = securePassword)
                    db.userDao().insertUser(newUser)

                    Toast.makeText(this@SignupActivity, "Account Created!", Toast.LENGTH_SHORT).show()

                    // Redirect to Login with NEW USER flag
                    val intent = Intent(this@SignupActivity, SigninActivity::class.java)
                    intent.putExtra("IS_NEW_USER", true) // <--- FLAG ADDED HERE
                    startActivity(intent)
                    finish()
                }
            }
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}