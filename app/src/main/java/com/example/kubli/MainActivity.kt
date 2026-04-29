package com.example.kubli

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        // ADDED: Check session before navigating
        val sharedPref = getSharedPreferences("KubliSession", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("IS_LOGGED_IN", false)

        // Wait 3 seconds, then go to appropriate screen
        Handler(Looper.getMainLooper()).postDelayed({

            if (isLoggedIn) {
                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, LoginsignupActivity::class.java)
                startActivity(intent)
            }

            finish()
        }, 3000)
    }
}