package com.example.kubli

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.imageview.ShapeableImageView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.ImageDecoder.decodeBitmap
import com.example.kubli.backend.SteganographyAPI
import android.graphics.ImageDecoder.createSource

class Encodeimage : AppCompatActivity() {

    private lateinit var btnMenu: ImageView
    private lateinit var imgEncodedResult: ShapeableImageView
    private lateinit var btnSaveImage: MaterialButton
    private lateinit var btnStartNewTask: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encodeimage)

        // Initialize Views
        btnMenu = findViewById(R.id.btnMenu)
        imgEncodedResult = findViewById(R.id.imgEncodedResult)
        btnSaveImage = findViewById(R.id.btnSaveImage)
        btnStartNewTask = findViewById(R.id.btnStartNewTask)

        //Retrieve the incoming Image URI and display it
        // (This gets the data passed from Encodemessage.kt)
        val imageUriString = intent.getStringExtra("IMAGE_URI")
        val originalText = intent.getStringExtra("ORIGINAL_TEXT") ?: ""
        val passwordInput = intent.getStringExtra("PASSWORD") ?: ""
        val password = passwordInput.ifEmpty { "demo1234" }

        if (imageUriString != null) {
            val imageUri = Uri.parse(imageUriString)

            // Encode using SteganographyAPI
            val api = SteganographyAPI(this)
            lifecycleScope.launch {
                try {
                    // Decode bitmap on a background thread
                    val bitmap: Bitmap = withContext(Dispatchers.IO) {
                        decodeBitmap(createSource(contentResolver, imageUri))
                    }

                    val result = api.encryptImage(originalText, password, bitmap)
                    if (result.success) {
                        imgEncodedResult.setImageBitmap(result.stegoBitmap)
                        imgEncodedResult.visibility = ImageView.VISIBLE
                        Toast.makeText(
                            this@Encodeimage,
                            "Encoded image ready!",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@Encodeimage,
                            "Encoding failed: ${result.error}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        this@Encodeimage,
                        "Error processing image: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            Toast.makeText(this, "No image received", Toast.LENGTH_SHORT).show()
            finish()
        }

        //Back/Menu Button
        btnMenu.setOnClickListener {
            finish()
        }

        //Save Image Button
        btnSaveImage.setOnClickListener {
            // TODO: Implement actual MediaStore image saving logic here
            Toast.makeText(this, "Saving encoded image to gallery...", Toast.LENGTH_SHORT).show()
        }

        // Start New Task Button
        btnStartNewTask.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}
