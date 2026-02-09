package com.example.kubli

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class GettingStartedActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_getting_started)

        val btnLetsStart = findViewById<Button>(R.id.btnLetsStart)

        btnLetsStart.setOnClickListener {
            val intent = Intent(this, PrivacyCheckActivity::class.java)
            startActivity(intent)

            finish()
        }
    }
}