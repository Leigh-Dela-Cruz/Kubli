package com.example.kubli

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class Encodemessage : AppCompatActivity() {

    private lateinit var imgPreview: ImageView
    private var selectedImageUri: Uri? = null

    // Image Picker
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            selectedImageUri = data?.data

            if (selectedImageUri != null) {
                imgPreview.setImageURI(selectedImageUri)

                // Adjust layout to fill the dashed box
                imgPreview.layoutParams.width = LinearLayout.LayoutParams.MATCH_PARENT
                imgPreview.layoutParams.height = LinearLayout.LayoutParams.MATCH_PARENT
                imgPreview.scaleType = ImageView.ScaleType.CENTER_CROP
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_encodemessage)

        // Initialize Views
        imgPreview = findViewById(R.id.imgPreview)
        val uploadArea = findViewById<LinearLayout>(R.id.layoutUploadClick)
        val btnHide = findViewById<Button>(R.id.btnHideMessage)
        val btnMenu = findViewById<ImageView>(R.id.btnMenu)
        val inputText = findViewById<EditText>(R.id.inputText)

        //Back Button
        btnMenu.setOnClickListener {
            finish()
        }

        //Upload Area Click
        uploadArea.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        // Hide Message Button
        btnHide.setOnClickListener {
            val message = inputText.text.toString().trim()
            val hasImage = selectedImageUri != null
            val hasText = message.isNotEmpty()

            // Check if BOTH are empty
            if (!hasImage && !hasText) {
                Toast.makeText(this, "Please upload an image OR enter text", Toast.LENGTH_SHORT).show()
            }
            // If user entered TEXT (even if they also uploaded an image for now), go to Encodetext flow
            else if (hasText) {
                Toast.makeText(this, "Encoding Text...", Toast.LENGTH_SHORT).show()

                // Create Intent to go to the new activity
                val intent = Intent(this, Encodetext::class.java)
                // Pass the original message text
                intent.putExtra("ORIGINAL_TEXT", message)
                startActivity(intent)
            }
            // If they ONLY uploaded an image (no text)
            else if (hasImage && !hasText) {
                Toast.makeText(this, "Image encoding flow not implemented yet", Toast.LENGTH_SHORT).show()
                // redirect to an image-specific result screen here later
            }
        }
    }
}