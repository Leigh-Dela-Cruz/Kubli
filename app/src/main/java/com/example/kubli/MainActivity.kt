package com.example.kubli

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        // Wait 3 seconds, then go to Choice Screen
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginsignupActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}