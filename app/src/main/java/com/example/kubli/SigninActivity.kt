package com.example.kubli

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


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
            val intent = Intent(this, UpdatePasswordActivity::class.java)
            startActivity(intent)
        }

        // Sign In Button
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)

        btnSignIn.setOnClickListener {
            val emailLayout = findViewById<TextInputLayout>(R.id.inputUser)
            val passLayout = findViewById<TextInputLayout>(R.id.inputPassword)

            val inputRaw = emailLayout.editText?.text.toString().trim()
            val password = passLayout.editText?.text.toString().trim()

            if (inputRaw.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter credentials", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val inputEmail = inputRaw.lowercase()

            // Firebase Login Verification
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()

            auth.signInWithEmailAndPassword(inputEmail, password)
                .addOnSuccessListener {

                    val uid = auth.currentUser!!.uid

                    firestore.collection("users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { document ->

                            if (document.exists()) {

                                val name = document.getString("fullName") ?: "User"
                                val email = document.getString("email") ?: inputEmail
                                val role = document.getString("role") ?: "user"

                                val sharedPref = getSharedPreferences("KubliSession", Context.MODE_PRIVATE)

                                with(sharedPref.edit()) {
                                    putString("CURRENT_USERNAME", name)
                                    putString("USER_NAME", name)
                                    putString("USER_EMAIL", email)
                                    putBoolean("IS_LOGGED_IN", true)
                                    putBoolean("IS_ADMIN", role == "admin")
                                    apply()
                                }


                                emailLayout.error = null
                                passLayout.error = null


                                if (role == "admin") {

                                    Toast.makeText(
                                        this,
                                        "Admin Login Successful",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    startActivity(
                                        Intent(
                                            this,
                                            AdminDashboardActivity::class.java
                                        )
                                    )

                                } else {

                                    Toast.makeText(
                                        this,
                                        "Login Successful",
                                        Toast.LENGTH_SHORT
                                    ).show()


                                    val isNewUser =
                                        intent.getBooleanExtra("IS_NEW_USER", false)

                                    if (isNewUser) {

                                        startActivity(
                                            Intent(
                                                this,
                                                LandingActivity::class.java
                                            )
                                        )

                                    } else {

                                        startActivity(
                                            Intent(
                                                this,
                                                HomeActivity::class.java
                                            )
                                        )
                                    }
                                }

                                finish()

                            } else {
                                Toast.makeText(
                                    this,
                                    "User data not found",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                }
                .addOnFailureListener {

                    passLayout.error = "Incorrect Email or Password"

                }
        }
    }
}
