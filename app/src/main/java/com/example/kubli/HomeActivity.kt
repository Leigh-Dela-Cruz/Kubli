package com.example.kubli

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // RECEIVE & DISPLAY USERNAME
        val sharedPref = getSharedPreferences("KubliSession", Context.MODE_PRIVATE)
        val username = sharedPref.getString("CURRENT_USERNAME", "User") ?: "User"

        val textGreeting = findViewById<TextView>(R.id.textGreeting)
        textGreeting.text = "Welcome, $username!"

        // BOTTOM NAVIGATION LOGIC
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true // Already on Home

                R.id.nav_profile -> {
                    val intent = Intent(this, UserProfileActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0) // Disables animation
                    finish() // Closes Home so no stack endless pages
                    true
                }

                R.id.nav_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }

        // ==========================================
        // 3D RISE ANIMATION & CLICK LOGIC
        // ==========================================
        val cardEncode = findViewById<MaterialCardView>(R.id.cardEncode)
        val cardDecode = findViewById<MaterialCardView>(R.id.cardDecode)

        applyRiseEffect(cardEncode, Encodemessage::class.java)
        applyRiseEffect(cardDecode, Decodemessage::class.java)
    }

    /**
     * Creates a smooth 3D lifting effect when the user touches the card,
     * then opens the target Activity when they release their finger.
     */
    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private fun applyRiseEffect(card: MaterialCardView, targetActivity: Class<*>) {
        card.setOnTouchListener { view, event ->
            when (event.action) {
                // PRESSED DOWN: Swell up 3% and lift off the page
                MotionEvent.ACTION_DOWN -> {
                    view.animate()
                        .scaleX(1.03f)
                        .scaleY(1.03f)
                        .translationZ(12f)
                        .setDuration(100)
                        .start()
                }
                // RELEASED: Drop back down smoothly
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationZ(0f)
                        .setDuration(150)
                        .start()

                    // If it was a clean tap (not a drag-cancel), open the new screen
                    if (event.action == MotionEvent.ACTION_UP) {
                        val intent = Intent(this@HomeActivity, targetActivity)
                        startActivity(intent)
                    }
                }
            }
            true
        }
    }
}