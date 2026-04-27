package com.example.kubli

import android.content.Context
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

            // FIXED: ensure consistent email format for login/signup matching
            val email = emailLayout.editText?.text.toString().trim().lowercase()

            val password = passLayout.editText?.text.toString().trim()

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.length > 150) {
                Toast.makeText(this, "Email must not exceed 150 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val allowedEmails = listOf(
                "gmail.com",
                "yahoo.com",
                "outlook.com",
                "hotmail.com",
                "icloud.com"
            )

            val domain = email.substringAfter("@")

            if (domain !in allowedEmails) {
                Toast.makeText(
                    this,
                    "Please use a valid email address",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Validate password strength
            val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$")

            if (!passwordPattern.matches(password)) {
                Toast.makeText(
                    this,
                    "Password must be at least 8 characters and include uppercase, lowercase, a number, and a symbol",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Database Operations
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(applicationContext)

                try {
                    val existingUser = db.userDao().getUserByEmail(email)

                    if (existingUser != null) {
                        Toast.makeText(this@SignupActivity, "Email already exists!", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // Hash Password & Insert User
                    val securePassword = hashPassword(password)

                    val newUser = User(
                        fullName = name,
                        email = email,
                        passwordHash = securePassword,
                        age = null
                    )

                    db.userDao().insertUser(newUser)

                    // DEBUG CHECK (does not affect logic)
                    val testUser = db.userDao().getUserByEmail(email)
                    android.util.Log.d("SIGNUP_DEBUG", "Inserted user = $testUser")

                    Toast.makeText(this@SignupActivity, "Account Created!", Toast.LENGTH_SHORT).show()

                    // bug1 fix: save sessions:
                    val sharedPref = getSharedPreferences("KubliSession", Context.MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putString("CURRENT_USERNAME", name)
                        putString("USER_NAME", name)
                        putString("USER_EMAIL", email)
                        putBoolean("IS_LOGGED_IN", true)
                        commit() // Force instant save
                    }

                    // Redirect directly to GettingStarted for new users
                    val intent = Intent(this@SignupActivity, GettingStartedActivity::class.java)
                    startActivity(intent)
                    finish()

                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(
                        this@SignupActivity,
                        "Signup failed. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}