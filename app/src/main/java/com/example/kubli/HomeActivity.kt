package com.example.kubli

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)


        // RECEIVE & DISPLAY USERNAME
        val sharedPref = getSharedPreferences("KubliSession", Context.MODE_PRIVATE)
        val username = sharedPref.getString("CURRENT_USERNAME", "User") ?: "User"

        val textGreeting = findViewById<TextView>(R.id.textGreeting)
        textGreeting.text = "Hi! $username"


        // BOTTOM NAVIGATION
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_profile -> {
                    Toast.makeText(this, "Opening Profile...", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Opening Settings...", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

       //HANDLE CARD CLICKS

        // Encode Card Click -> Opens Encodemessage Activity
        findViewById<android.view.View>(R.id.cardEncode).setOnClickListener {
            val intent = Intent(this, Encodemessage::class.java)
            startActivity(intent)
        }

        // Decode Card Click -> Opens DecodeMessage Activity
        findViewById<android.view.View>(R.id.cardDecode).setOnClickListener {
            val intent = Intent(this, Decodemessage::class.java)
            startActivity(intent)
        }
    }
}