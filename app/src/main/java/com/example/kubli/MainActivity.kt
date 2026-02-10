package com.example.kubli

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(this))
                Log.d("PythonInit", "Python started successfully")
            } else {
                Log.d("PythonInit", "Python already started")
            }
        } catch (e: Exception) {
            Log.e("PythonInit", "Python initialization failed", e)
        }

        // Wait 3 seconds, then go to Choice Screen
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, LoginsignupActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}