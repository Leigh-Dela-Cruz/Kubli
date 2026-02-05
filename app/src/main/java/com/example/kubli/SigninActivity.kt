package com.example.kubli

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SigninActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        // 1. Handle "Forgot Password" Underline & Click
        val textForgot = findViewById<TextView>(R.id.textForgotPassword)
        textForgot.paintFlags = textForgot.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        textForgot.setOnClickListener {
            Toast.makeText(this, "Forgot Password clicked", Toast.LENGTH_SHORT).show()
        }

        // 2. Handle "Register Now" (Footer)
        val textRegister = findViewById<TextView>(R.id.textRegister)
        textRegister.setOnClickListener {
            // Open the Signup Activity
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            finish() // Close sign in so back button doesn't loop
        }

        // 3. Handle Sign In Button
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        btnSignIn.setOnClickListener {
            Toast.makeText(this, "Signing in...", Toast.LENGTH_SHORT).show()
        }
    }
}