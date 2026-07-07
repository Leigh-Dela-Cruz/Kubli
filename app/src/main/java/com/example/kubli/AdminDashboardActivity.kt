package com.example.kubli

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

class AdminDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)

        // This listener handles the clicks on your bottom navigation items
        bottomNav.setOnItemSelectedListener { item ->
            // Note: Update these R.id values to match the IDs in  chosen menu file
            when (item.itemId) {
                R.id.nav_home -> {
                    // Already on the dashboard
                    true
                }
                R.id.nav_profile -> {
                    Toast.makeText(this, "Opening Profile...", Toast.LENGTH_SHORT).show()
                    // Add  intent to navigate to the profile activity here
                    true
                }
                R.id.nav_settings -> {
                    Toast.makeText(this, "Opening Settings...", Toast.LENGTH_SHORT).show()
                    // Add  intent to navigate to the settings activity here
                    true
                }
                else -> false
            }
        }
    }
}