package com.example.kubli

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class UserProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_userprofile)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnEditProfile = findViewById<ImageView>(R.id.btnEditProfile)
        val btnLogout = findViewById<LinearLayout>(R.id.btnLogout)
        val cardChangePassword = findViewById<MaterialCardView>(R.id.cardChangePassword)

        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val tvNameValue = findViewById<TextView>(R.id.tvNameValue)
        val tvEmailValue = findViewById<TextView>(R.id.tvEmailValue)
        val tvAgeValue = findViewById<TextView>(R.id.tvAgeValue)

        // FETCH USERNAME FROM SESSION (Matches HomeActivity logic)
        val sharedPref = getSharedPreferences("KubliSession", Context.MODE_PRIVATE)
        val username = sharedPref.getString("CURRENT_USERNAME", "Username") ?: "Username"
        val name = sharedPref.getString("USER_NAME", "Username") ?: "Username"
        val email = sharedPref.getString("USER_EMAIL", "Username@email.com") ?: "Username@email.com"
        val age = sharedPref.getString("USER_AGE", "") ?: "" // Defaults to completely blank

        // Update the UI with the fetched username
        tvUsername.text = username
        tvNameValue.text = username
        tvEmailValue.text = email
        tvAgeValue.text = age

        // Handle Back Navigation
        btnBack.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            startActivity(intent)
            finish()
        }

        // Navigate to Edit Profile
        btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        // Navigate to Login/Signup on Log out
        btnLogout.setOnClickListener {
            // FIX: Clear the session data
            val sharedPref = getSharedPreferences("KubliSession", Context.MODE_PRIVATE)
            sharedPref.edit().clear().apply()
            val intent = Intent(this, LoginsignupActivity::class.java)

            // Clear the back stack so user can't press back to return to profile
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Change Password Placeholder
        cardChangePassword.setOnClickListener {
            val intent = Intent(this, UpdatePasswordActivity::class.java)
            startActivity(intent)
        }

        // BOTTOM NAVIGATION
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_profile

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0) // Disables animation for seamless tab switch
                    finish() // Prevents stacking activities endlessly
                    true
                }
                R.id.nav_profile -> true // Already on Profile
                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finishAffinity()
                    true
                }
                else -> false
            }
        }
    }
}