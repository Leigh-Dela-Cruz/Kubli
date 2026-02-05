package com.example.kubli

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // 1. Setup "Sign In" text to Navigate to SigninActivity
        val textSignIn = findViewById<TextView>(R.id.textSignIn)
        textSignIn.setOnClickListener {
            val intent = Intent(this, SigninActivity::class.java)
            startActivity(intent)

            finish()
        }

        // 2. Setup Social Buttons
        val btnGoogle = findViewById<ImageView>(R.id.btnGoogle)
        btnGoogle.setOnClickListener {
            Toast.makeText(this, "Google Sign Up Clicked", Toast.LENGTH_SHORT).show()
        }

        val btnApple = findViewById<ImageView>(R.id.btnApple)
        btnApple.setOnClickListener {
            Toast.makeText(this, "Apple Sign Up Clicked", Toast.LENGTH_SHORT).show()
        }

        // 3. Setup Create Account Button
        val btnCreate = findViewById<android.view.View>(R.id.btnCreateAccount)
        btnCreate.setOnClickListener {
            Toast.makeText(this, "Creating Account...", Toast.LENGTH_SHORT).show()
        }
    }
}