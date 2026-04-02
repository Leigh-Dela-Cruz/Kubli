package com.example.kubli

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kubli.backend.SteganographyAPI
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.launch

class Decodeimage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_decodeimage)

        // Get Views
        val btnMenu = findViewById<ImageView>(R.id.btnMenu)
        val imgDecryptedResult = findViewById<ShapeableImageView>(R.id.imgDecryptedResult)
        val textDecodedResult = findViewById<TextView>(R.id.textDecodedResult)
        val btnCopyText = findViewById<MaterialButton>(R.id.btnCopyText)
        val btnStartNewTask = findViewById<MaterialButton>(R.id.btnStartNewTask)

        //Receive data from previous activity
        val imageUriString = intent.getStringExtra("IMAGE_URI")
        val passwordInput = intent.getStringExtra("PASSWORD") ?: ""
        val password = passwordInput.ifEmpty { "demo1234" }

        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)
            imgDecryptedResult.setImageURI(imageUri)

            lifecycleScope.launch {
                try {
                    val api = SteganographyAPI(this@Decodeimage)

                    val result = api.decryptImage(
                        context = this@Decodeimage,
                        imageUri = imageUri,
                        password = password
                    )

                    textDecodedResult.text = if (result.success) {
                        result.message ?: "Decoded, but message is empty."
                    } else {
                        "Decoding failed: ${result.error ?: "Unknown error"}"
                    }
                } catch (e: Exception) {
                    textDecodedResult.text = "Crash prevented: ${e.localizedMessage}"
                }
            }
        }

        // Back Button
        btnMenu.setOnClickListener {
            finish()
        }

        // Copy Text Button
        btnCopyText.setOnClickListener {
            val decodedText = textDecodedResult.text.toString()

            // Access Android's Clipboard Manager
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Decoded Message", decodedText)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
        }

        //Start New Task Logic
        btnStartNewTask.setOnClickListener {
            // This clears the backstack and sends the user cleanly back to HomeActivity
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}