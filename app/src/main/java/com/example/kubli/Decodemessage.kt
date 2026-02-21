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

class Decodemessage : AppCompatActivity() {

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
        setContentView(R.layout.activity_decodemessage)

        // Initialize Views
        imgPreview = findViewById(R.id.imgPreview)
        val uploadArea = findViewById<LinearLayout>(R.id.layoutUploadClick)
        val btnExtract = findViewById<Button>(R.id.btnExtractMessage)
        val btnMenu = findViewById<ImageView>(R.id.btnMenu)
        val inputText = findViewById<EditText>(R.id.inputText)

        // Back Button
        btnMenu.setOnClickListener {
            finish()
        }

        // Upload Area Click
        uploadArea.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            pickImageLauncher.launch(intent)
        }

        // Extract Message Button
        btnExtract.setOnClickListener {
            val message = inputText.text.toString().trim()
            val hasImage = selectedImageUri != null
            val hasText = message.isNotEmpty()

            // Check if BOTH are empty
            if (!hasImage && !hasText) {
                Toast.makeText(this, "Please upload an image OR enter text", Toast.LENGTH_SHORT).show()
            }
            // Check if BOTH are filled
            else if (hasImage && hasText) {
                Toast.makeText(this, "Error: Please choose ONLY ONE (Image OR Text). Clear one to proceed.", Toast.LENGTH_LONG).show()
            }
            // ONLY Image is provided
            else if (hasImage) {
                Toast.makeText(this, "Processing Image...", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, Decodeimage::class.java)
                intent.putExtra("IMAGE_URI", selectedImageUri.toString())
                startActivity(intent)
            }
            // ONLY Text is provided
            else if (hasText) {
                Toast.makeText(this, "Processing Text...", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, Decodetext::class.java)
                intent.putExtra("TEXT_TO_DECODE", message)
                startActivity(intent)
            }
        }
        }
    }
