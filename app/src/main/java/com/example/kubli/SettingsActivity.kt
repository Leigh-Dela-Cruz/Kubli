package com.example.kubli

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Toolbar
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // Row Layouts
        val rowTerms = findViewById<RelativeLayout>(R.id.rowTerms)
        val rowPrivacy = findViewById<RelativeLayout>(R.id.rowPrivacy)
        val rowVersion = findViewById<RelativeLayout>(R.id.rowVersion)
        val rowContact = findViewById<RelativeLayout>(R.id.rowContact)

        // Handle Back Navigation
        btnBack.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        rowTerms.setOnClickListener {
            val intent = Intent(this, TermsAndConditionsActivity::class.java)
            intent.putExtra("IS_FROM_SETTINGS", true)
            startActivity(intent)
        }

        rowPrivacy.setOnClickListener {
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            intent.putExtra("IS_FROM_SETTINGS", true)
            startActivity(intent)
        }

        rowVersion.setOnClickListener {
            Toast.makeText(this, "App Version 1.0.0", Toast.LENGTH_SHORT).show()
        }

        rowContact.setOnClickListener {
            Toast.makeText(this, "Contact Us clicked", Toast.LENGTH_SHORT).show()
        }

        // BOTTOM NAVIGATION LOGIC
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_settings // Keep Settings tab highlighted

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0) // Disables slide animation
                    finishAffinity()
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, UserProfileActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finishAffinity()
                    true
                }
                R.id.nav_settings -> true // Already on Settings
                else -> false
            }
        }
    }
}