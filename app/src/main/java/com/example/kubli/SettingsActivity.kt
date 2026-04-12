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
        val rowNotifications = findViewById<RelativeLayout>(R.id.rowNotifications)
        val rowLinkedAccounts = findViewById<RelativeLayout>(R.id.rowLinkedAccounts)
        val switchDarkMode = findViewById<SwitchMaterial>(R.id.switchDarkMode)
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

        // Click Listeners (Placeholders for now)
        rowNotifications.setOnClickListener {
            Toast.makeText(this, "Notification Preferences clicked", Toast.LENGTH_SHORT).show()
        }

        rowLinkedAccounts.setOnClickListener {
            Toast.makeText(this, "Linked Accounts clicked", Toast.LENGTH_SHORT).show()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "enabled" else "disabled"
            Toast.makeText(this, "Dark Mode $status", Toast.LENGTH_SHORT).show()
            // Future implementation: Actually switch the app theme here
        }

        //Removed the double nested listeners here
        rowTerms.setOnClickListener {
            val intent = Intent(this, TermsandconditionsActivity::class.java)
            startActivity(intent)
        }

        //Removed the double nested listeners here
        rowPrivacy.setOnClickListener {
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
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