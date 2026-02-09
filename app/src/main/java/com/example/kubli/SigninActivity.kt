package com.example.kubli

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.security.MessageDigest

class SigninActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        // Redirect to Register
        val textRegister = findViewById<TextView>(R.id.textRegister)
        textRegister.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Forgot Password link
        val textForgot = findViewById<TextView>(R.id.textForgotPassword)
        textForgot.paintFlags = textForgot.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        textForgot.setOnClickListener {
            Toast.makeText(this, "Forgot Password Clicked", Toast.LENGTH_SHORT).show()
        }

        // Sign In Button
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)

        btnSignIn.setOnClickListener {
            val emailLayout = findViewById<TextInputLayout>(R.id.inputUser)
            val passLayout = findViewById<TextInputLayout>(R.id.inputPassword)

            val email = emailLayout.editText?.text.toString().trim()
            val password = passLayout.editText?.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter credentials", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Database Verification
            lifecycleScope.launch {
                val db = AppDatabase.getDatabase(applicationContext)
                val user = db.userDao().getUserByEmail(email)

                if (user != null) {
                    val inputHash = hashPassword(password)
                    if (inputHash == user.passwordHash) {

                        // Login Success
                        Toast.makeText(this@SigninActivity, "Welcome back, ${user.fullName}!", Toast.LENGTH_SHORT).show()

                        // Save User Session
                        val sharedPref = getSharedPreferences("KubliSession", Context.MODE_PRIVATE)
                        with (sharedPref.edit()) {
                            putString("CURRENT_USERNAME", user.fullName)
                            putBoolean("IS_LOGGED_IN", true)
                            apply()
                        }

                        // CHECK ROUTE: New User vs Returning User
                        val isNewUser = intent.getBooleanExtra("IS_NEW_USER", false)

                        if (isNewUser) {
                            // New User -> Go to Landing (Onboarding)
                            val intent = Intent(this@SigninActivity, LandingActivity::class.java)
                            startActivity(intent)
                        } else {
                            // Returning User -> Go to Dashboard directly
                            val intent = Intent(this@SigninActivity, HomeActivity::class.java)
                            startActivity(intent)
                        }
                        finish()

                    } else {
                        passLayout.error = "Incorrect Password"
                    }
                } else {
                    emailLayout.error = "User not found"
                }
            }
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}